package industries.leeway.pocket

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object FoldDigitalBrain {
    private const val FILE="phone-fold6-digital-brain.json"
    fun bootstrap(context:Context){
        val root=File(context.filesDir,"digital-brain");root.mkdirs()
        val pkg=context.packageManager
        val apps=JSONArray()
        pkg.getInstalledApplications(0).take(500).forEach{apps.put(it.packageName)}
        val stat=StatFs(Environment.getDataDirectory().path)
        val device=JSONObject()
            .put("manufacturer",Build.MANUFACTURER).put("model",Build.MODEL)
            .put("device",Build.DEVICE).put("android",Build.VERSION.RELEASE)
            .put("sdk",Build.VERSION.SDK_INT).put("storageBytes",stat.totalBytes)
            .put("storageAvailableBytes",stat.availableBytes)
        val info=JSONObject().put("installedPackages",apps).put("authorizedRoots",JSONArray().put(context.filesDir.absolutePath))
        val runtime=JSONObject()
            .put("agentId","agent-lee").put("bodyId","phone-fold6")
            .put("deviceBridgePackage","industries.leeway.devicebridge")
            .put("voiceAuthority","4citeB4U/LeeWay-Voice-Fabric")
            .put("skillsAuthority","4citeB4U/LeeWay-Agent-Skills")
            .put("formulaAuthority","4citeB4U/Leeway-formula-live")
            .put("continuumState","BINDING_REQUIRED").put("ldwmdState","BINDING_REQUIRED")
        val brain=JSONObject().put("schemaVersion","leeway.digital-brain.v1").put("digitalBrainId","digital-brain:phone-fold6")
            .put("bodyId","phone-fold6").put("informationBrain",info).put("deviceBrain",device).put("runtimeBrain",runtime)
            .put("updatedAt",System.currentTimeMillis())
        File(root,FILE).writeText(brain.toString(2))
    }
    fun snapshot(context:Context):String{
        val f=File(File(context.filesDir,"digital-brain"),FILE)
        return if(f.exists())f.readText() else JSONObject().put("state","UNAVAILABLE").toString()
    }
}
