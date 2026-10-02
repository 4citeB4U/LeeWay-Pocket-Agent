package industries.leeway.pocket

import android.content.Context
import android.webkit.JavascriptInterface
import ai.onnxruntime.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Canonical conditional decoder only. CPU execution, no navigation or file API. */
class NativeVoiceDecoder(context:Context, private val trusted:()->Boolean, private val emit:(JSONObject)->Unit) {
    private val directory=File(context.applicationContext.filesDir,"voice-one-decoder")
    private val executor=Executors.newSingleThreadExecutor()
    private val timer=Executors.newSingleThreadScheduledExecutor()
    private val active=AtomicReference<NativeDecoderJob?>(null)
    private val runLock=Any()
    private var runOptions:OrtSession.RunOptions?=null
    private var session:OrtSession?=null
    private var encoderSession:OrtSession?=null
    @Volatile private var closed=false
    private val environment by lazy { OrtEnvironment.getEnvironment() }
    private val base="https://huggingface.co/onnx-community/chatterbox-ONNX/resolve/3cab09af388d3f02bba43443fce88c1f4525ac43/onnx/"
    private data class Asset(val name:String,val bytes:Long,val hash:String)
    private val decoderAssets=listOf(
        Asset("conditional_decoder.onnx",6350448,"1656d0d31332bae1854839959a3139300ebb67c178651dfa3f8c5fbfa5351351"),
        Asset("conditional_decoder.onnx_data",533970816,"51d58345a272747665ec9d5bb61e01835258a940e321a288582ac4c18cf01b5a")
    )
    private val encoderAssets=listOf(
        Asset("speech_encoder.onnx",1184608,"8f1c8a0f89b77bf9cd5dd8f2e034eb2c79dc00fe70d41196b28c257643b00ccb"),
        Asset("speech_encoder.onnx_data",591274880,"04431dcef6325c54b02de2219845888b464bcd1f1ac2f8839c2fecd1ed2ef294")
    )
    @JavascriptInterface fun prepare(id:String)=submit(id,20*60){job->
        directory.mkdirs()
        for(asset in encoderAssets){job.check();ensureAsset(asset,job,"Downloading canonical native speech encoder")}
        for(asset in decoderAssets){job.check();ensureAsset(asset,job,"Downloading canonical native decoder")}
        if(session==null){
            progress(job,"Initializing native CPU decoder")
            val loaded=createSession(decoderAssets[0].name)
            if(job.cancelled.get()||closed){loaded.close();job.check();error("DECODER_CLOSED")}
            session=loaded
        }
        JSONObject().put("ready",true).put("backend","onnxruntime-android-cpu")
            .put("speechEncoder","onnxruntime-android-cpu").put("threads",4)
    }
    @JavascriptInterface fun decode(id:String,payload:String)=submit(id,180){job->
        val inputs=NativeDecoderContract.decode(payload)
        val decoder=session?:error("DECODER_NOT_READY")
        val tensors=linkedMapOf<String,OnnxTensor>()
        try{
            for(input in inputs){
                val buffer=ByteBuffer.allocateDirect(input.bytes.size).order(ByteOrder.LITTLE_ENDIAN).put(input.bytes)
                buffer.rewind()
                tensors[input.name]=if(input.int64)OnnxTensor.createTensor(environment,buffer.asLongBuffer(),input.dims)
                    else OnnxTensor.createTensor(environment,buffer.asFloatBuffer(),input.dims)
            }
            OrtSession.RunOptions().use { options ->
                synchronized(runLock){job.check();runOptions=options}
                try{
                    val start=System.nanoTime()
                    decoder.run(tensors,options).use { result ->
                        job.check()
                        val output=result.get("waveform").orElseThrow() as OnnxTensor
                        val samples=output.floatBuffer
                        require(samples.remaining() in 1..NativeDecoderContract.MAX_SAMPLES){"DECODER_OUTPUT_TOO_LARGE"}
                        val count=samples.remaining();var energy=0.0;var peak=0.0
                        val bytes=ByteBuffer.allocate(samples.remaining()*4).order(ByteOrder.LITTLE_ENDIAN)
                        while(samples.hasRemaining()){val sample=samples.get();require(sample.isFinite()){"DECODER_NONFINITE_OUTPUT"};energy+=sample.toDouble()*sample;peak=maxOf(peak,kotlin.math.abs(sample.toDouble()));bytes.putFloat(sample)}
                        val rms=kotlin.math.sqrt(energy/count)
                        require(peak>1e-6 && rms>1e-7){"DECODER_SILENT_OUTPUT"}
                        android.util.Log.i("LeeWayPocketVoice","NATIVE_DECODER waveform samples=$count rms=$rms peak=$peak elapsedMs=${(System.nanoTime()-start)/1_000_000}")
                        JSONObject().put("dtype","float32").put("dims",JSONArray((output.info as TensorInfo).shape.toList()))
                            .put("data",Base64.getEncoder().encodeToString(bytes.array()))
                            .put("rms",rms).put("peak",peak)
                            .put("elapsedMs",(System.nanoTime()-start)/1_000_000)
                    }
                }finally{synchronized(runLock){runOptions=null}}
            }
        }finally{tensors.values.forEach{it.close()}}
    }
    @JavascriptInterface fun encode(id:String,payload:String)=submit(id,300){job->
        val input=NativeEncoderContract.decode(payload)
        progress(job,"Initializing canonical native speech encoder")
        val encoder=createSession(encoderAssets[0].name)
        encoderSession=encoder
        val buffer=ByteBuffer.allocateDirect(input.bytes.size).order(ByteOrder.LITTLE_ENDIAN).put(input.bytes)
        buffer.rewind()
        val tensor=OnnxTensor.createTensor(environment,buffer.asFloatBuffer(),input.dims)
        try{
            OrtSession.RunOptions().use { options ->
                synchronized(runLock){job.check();runOptions=options}
                try{
                    val start=System.nanoTime()
                    encoder.run(mapOf(input.name to tensor),options).use { result ->
                        job.check()
                        val expected=linkedSetOf("audio_features","audio_tokens","speaker_embeddings","speaker_features")
                        require(encoder.outputNames==expected){"ENCODER_OUTPUT_NAMES"}
                        val out=JSONObject();var totalBytes=0
                        for(name in expected){
                            val output=result.get(name).orElseThrow() as OnnxTensor
                            val info=output.info as TensorInfo
                            val dims=info.shape
                            val count=dims.fold(1L){a,b->a*b}
                            require(count in 1L..2_000_000L){"ENCODER_OUTPUT_SHAPE"}
                            val int64=name=="audio_tokens"
                            val bytes=ByteBuffer.allocate(Math.toIntExact(count*(if(int64)8L else 4L))).order(ByteOrder.LITTLE_ENDIAN)
                            if(int64){
                                val values=output.longBuffer
                                while(values.hasRemaining())bytes.putLong(values.get())
                            }else{
                                val values=output.floatBuffer
                                while(values.hasRemaining()){
                                    val value=values.get();require(value.isFinite()){"ENCODER_NONFINITE_OUTPUT"};bytes.putFloat(value)
                                }
                            }
                            totalBytes+=bytes.position()
                            require(totalBytes<=NativeEncoderContract.MAX_OUTPUT_BYTES){"ENCODER_OUTPUT_TOO_LARGE"}
                            out.put(name,JSONObject().put("dtype",if(int64)"int64" else "float32")
                                .put("dims",JSONArray(dims.toList()))
                                .put("data",Base64.getEncoder().encodeToString(bytes.array())))
                        }
                        android.util.Log.i("LeeWayPocketVoice","NATIVE_ENCODER outputs="+expected.joinToString(",")+" bytes="+totalBytes+" elapsedMs="+((System.nanoTime()-start)/1_000_000))
                        out
                    }
                }finally{synchronized(runLock){runOptions=null}}
            }
        }finally{
            tensor.close()
            encoder.close()
            encoderSession=null
        }
    }
    @JavascriptInterface fun cancel(id:String){
        active.get()?.takeIf{it.id==id}?.let { job ->
            job.cancel()
            synchronized(runLock){runCatching{runOptions?.setTerminate(true)}}
            complete(job,error="DECODER_CANCELLED")
        }
    }
    private fun submit(id:String,seconds:Long,work:(NativeDecoderJob)->JSONObject){
        synchronized(this){
        if(!id.matches(Regex("voice-[a-f0-9-]{36}-[0-9]+")))return
        if(closed||!trusted()){emit(JSONObject().put("id",id).put("error","DECODER_ORIGIN_UNAVAILABLE"));return}
        val job=NativeDecoderJob(id)
        if(!active.compareAndSet(null,job)){emit(JSONObject().put("id",id).put("error","DECODER_BUSY"));return}
        val deadline=timer.schedule({
            job.cancel();synchronized(runLock){runCatching{runOptions?.setTerminate(true)}}
            complete(job,error="DECODER_TIMEOUT")
        },seconds,TimeUnit.SECONDS)
        executor.execute {
            try{job.check();val result=work(job);job.check();complete(job,result)}
            catch(error:Exception){complete(job,error=error.message?.take(240)?:"DECODER_FAILED")}
            finally{deadline.cancel(false);active.compareAndSet(job,null)}
        }
        }
    }
    private fun complete(job:NativeDecoderJob,result:JSONObject?=null,error:String?=null){
        if(job.finish()&&!closed)emit(JSONObject().put("id",job.id).apply{if(error!=null)put("error",error) else put("result",result)})
    }
    private fun progress(job:NativeDecoderJob,message:String,loaded:Long?=null,total:Long?=null){
        if(!job.cancelled.get()&&!closed)emit(JSONObject().put("id",job.id).put("progress",JSONObject()
            .put("message",message).put("loaded",loaded).put("total",total)))
    }
    private fun digest(file:File,job:NativeDecoderJob):String {
        val hash=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream -> val bytes=ByteArray(128*1024);while(true){job.check();val n=stream.read(bytes);if(n<0)break;hash.update(bytes,0,n)} }
        return hash.digest().joinToString(""){"%02x".format(it)}
    }
    private fun createSession(graph:String):OrtSession = OrtSession.SessionOptions().use { options ->
        options.setIntraOpNumThreads(4)
        options.setInterOpNumThreads(1)
        options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
        options.setCPUArenaAllocator(false)
        environment.createSession(File(directory,graph).absolutePath,options)
    }
    private fun ensureAsset(asset:Asset,job:NativeDecoderJob,message:String){
        val target=File(directory,asset.name)
        if(target.isFile && target.length()==asset.bytes && digest(target,job)==asset.hash)return
        val part=File(directory,asset.name+".part")
        for(attempt in 1..2){
            var connection:HttpURLConnection?=null
            try{
                job.check();connection=URL(base+asset.name).openConnection() as HttpURLConnection
                connection.connectTimeout=30000;connection.readTimeout=30000
                require(connection.responseCode==200){"DECODER_DOWNLOAD_HTTP_${connection.responseCode}"}
                var count=0L;var lastProgress=0L
                connection.inputStream.use { input -> part.outputStream().use { output ->
                    val bytes=ByteArray(128*1024)
                    while(true){job.check();val n=input.read(bytes);if(n<0)break;count+=n;require(count<=asset.bytes){"DECODER_DOWNLOAD_SIZE"};output.write(bytes,0,n)
                        val now=System.currentTimeMillis();if(now-lastProgress>1000){progress(job,message,count,asset.bytes);lastProgress=now}
                    }
                } }
                require(count==asset.bytes && digest(part,job)==asset.hash){"DECODER_DOWNLOAD_INTEGRITY"}
                java.nio.file.Files.move(part.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                return
            }catch(error:Exception){part.delete();job.check();if(attempt==2)throw error}
            finally{connection?.disconnect()}
        }
    }
    fun cancelActive(){active.get()?.let{cancel(it.id)}}
    fun close()=synchronized(this){
        if(closed)return@synchronized
        closed=true;active.get()?.let{it.cancel()}
        synchronized(runLock){runCatching{runOptions?.setTerminate(true)}}
        timer.shutdownNow()
        executor.execute{encoderSession?.close();encoderSession=null;session?.close();session=null}
        executor.shutdown()
    }
}
