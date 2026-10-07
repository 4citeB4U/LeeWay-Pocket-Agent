/*
REGION: LEEWAY.BRAIN.ANDROID_PREVIEW
TAG: OWNER_SELECTED_READ_ONLY_CONTENT_VIEWER
5WH: WHO=Device owner; WHAT=View Brain-bound or owner-selected files; WHEN=explicit tap;
WHERE=existing Pocket package; WHY=restore original media/file preview affordance;
HOW=Android content permission and native read-only PDF/media/image/text primitives.
AUTHORIZED ROLES: OWNER_FILE_VIEWER. LICENSE: MIT.
No delete, modify, root traversal, background transfer or alternative filesystem authority.
*/
package industries.leeway.pocket

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption

class BrainFilePreviewActivity:Activity(){
    private var pdf:PdfRenderer?=null
    private var descriptor:ParcelFileDescriptor?=null
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val source=intent.data ?: run{finish();return}
        if(source.scheme!="content"&&source.scheme!="file"){finish();return}
        if(source.scheme=="file"){
            val target=runCatching{File(URI(source.toString())).toPath()}.getOrNull()
            val root=filesDir.toPath().toRealPath()
            if(target==null||Files.isSymbolicLink(target)||!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS)||
                !target.toRealPath().startsWith(root)){finish();return}
        }
        val rootView=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(5,12,22))}
        setContentView(rootView)
        val title=TextView(this).apply{setTextColor(Color.WHITE);textSize=16f;text="LeeWay Digital Brain • File viewer";setPadding(16,12,12,12)}
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val back=Button(this).apply{text="Back to Brain";setOnClickListener{finish()}}
        bar.addView(back)
        rootView.addView(bar)
        rootView.addView(title)
        val body=FrameLayout(this)
        rootView.addView(body,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        try{
            val name=source.lastPathSegment?.substringAfterLast('/')?:"File"
            title.text="LeeWay Digital Brain • "+name.take(80)
            val mime=(intent.type?:contentResolver.getType(source)?:mimeFromName(name)).lowercase()
            when{
                mime=="application/pdf"->showPdf(body,source)
                mime.startsWith("video/")||mime.startsWith("audio/")->{
                    val video=VideoView(this)
                    body.addView(video,FrameLayout.LayoutParams(-1,-1))
                    val controls=android.widget.MediaController(this)
                    controls.setAnchorView(video)
                    video.setMediaController(controls)
                    video.setVideoURI(source)
                    video.setOnPreparedListener{controls.show(60000)}
                    video.requestFocus()
                }
                mime.startsWith("image/")->{
                    val image=ImageView(this).apply{scaleType=ImageView.ScaleType.FIT_CENTER}
                    val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    contentResolver.openInputStream(source)?.use{BitmapFactory.decodeStream(it,null,options)}
                    val maximum=maxOf(options.outWidth,options.outHeight)
                    val sample=generateSequence(1){it*2}.takeWhile{it<=16}.lastOrNull{maximum/it>=2048}?:1
                    val decode=BitmapFactory.Options().apply{inSampleSize=sample}
                    contentResolver.openInputStream(source)?.use{image.setImageBitmap(BitmapFactory.decodeStream(it,null,decode))}
                    body.addView(image,FrameLayout.LayoutParams(-1,-1))
                }
                mime.startsWith("text/")||mime in setOf("application/json","application/xml","application/javascript","application/x-sh")->{
                    val input=contentResolver.openInputStream(source)?:error("FILE_READ_DENIED")
                    val bytes=input.use{it.readNBytes(1048577)}
                    check(bytes.size<=1048576){"TEXT_PREVIEW_LIMIT"}
                    val text=TextView(this).apply{
                        setTextColor(Color.WHITE);typeface=Typeface.MONOSPACE;textSize=13f
                        setPadding(16,16,16,16);setText(String(bytes,Charsets.UTF_8))
                    }
                    body.addView(ScrollView(this).apply{addView(HorizontalScrollView(this@BrainFilePreviewActivity).apply{addView(text)})})
                }
                else->showMessage(body,"No built-in viewer for this file format. Use the device's compatible application.")
            }
        }catch(error:Exception){showMessage(body,"Preview unavailable: "+(error.message?:"access denied").take(120))}
    }
    private fun showPdf(body:FrameLayout,uri:Uri){
        descriptor=if(uri.scheme=="file")ParcelFileDescriptor.open(File(URI(uri.toString())),ParcelFileDescriptor.MODE_READ_ONLY)
            else contentResolver.openFileDescriptor(uri,"r")
        pdf=PdfRenderer(descriptor?:error("PDF_OPEN_DENIED"))
        val viewer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val pager=LinearLayout(this).apply{gravity=Gravity.CENTER}
        val image=ImageView(this).apply{scaleType=ImageView.ScaleType.FIT_CENTER}
        val indicator=TextView(this).apply{setTextColor(Color.WHITE);setPadding(14,8,14,8)}
        var page=0
        fun render(){
            val document=pdf?:return
            val opened=document.openPage(page)
            try{
                val scale=minOf(2f,2400f/maxOf(opened.width,opened.height))
                val bitmap=Bitmap.createBitmap(maxOf(1,(opened.width*scale).toInt()),maxOf(1,(opened.height*scale).toInt()),Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                opened.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                image.setImageBitmap(bitmap)
                indicator.text="${page+1} / ${document.pageCount}"
            }finally{opened.close()}
        }
        pager.addView(Button(this).apply{text="Previous";setOnClickListener{if(page>0){page--;render()}}})
        pager.addView(indicator)
        pager.addView(Button(this).apply{text="Next";setOnClickListener{if(page+1<(pdf?.pageCount?:0)){page++;render()}}})
        viewer.addView(pager)
        viewer.addView(image,LinearLayout.LayoutParams(-1,0,1f))
        body.addView(viewer)
        render()
    }
    private fun showMessage(body:FrameLayout,message:String){body.removeAllViews();body.addView(TextView(this).apply{
        text=message;setTextColor(Color.WHITE);setPadding(20,20,20,20)
    })}
    private fun mimeFromName(name:String):String=when(name.substringAfterLast('.',"").lowercase()){
        "pdf"->"application/pdf";"mp4","m4v"->"video/mp4";"webm"->"video/webm"
        "mp3"->"audio/mpeg";"wav"->"audio/wav";"m4a"->"audio/mp4"
        "png"->"image/png";"jpg","jpeg"->"image/jpeg";"webp"->"image/webp"
        "json"->"application/json";"kt","py","js","ts","java","html","css","txt","md","xml","yaml","yml"->"text/plain"
        else->"application/octet-stream"
    }
    override fun onDestroy(){pdf?.close();descriptor?.close();super.onDestroy()}
}
