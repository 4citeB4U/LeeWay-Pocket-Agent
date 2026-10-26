/*
REGION: LEEWAY.DEVICES.DIAGNOSTICS
TAG: LOCAL_HARDWARE_BRAIN_OBSERVATIONS
WHO: Device owner; WHAT: Device-local hardware and connected-device observations for the Digital Brain.
WHEN: Diagnostics open/refresh; WHERE: Android native adapter, never shared core.
WHY: A connection badge or demo profile is not telemetry. Missing measurements remain unavailable.
HOW: Public native APIs and owner-granted permissions; no silent camera/mic capture or nearby scan.
FORMULA: Raw observations remain authority. Formula domain adapter is NOT_EXECUTED until calibrated.
LINEAGE: LEEWAY-DEVICE-BRIDGE DevicePassport.kt and Diagnostics.kt.
LICENSE: MIT
*/
package industries.leeway.pocket.devices

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import industries.leeway.brain.*
import industries.leeway.pocket.*
import org.json.JSONObject
import org.json.JSONArray

object DeviceDiagnostics {
    @Volatile private var lastCapture=0L
    fun capture(context:Context):Map<String,JSONObject> {
        fun observed(source:String,block:JSONObject.()->Unit):JSONObject=try{JSONObject().put("state","OBSERVED").put("source",source).put("capturedAtMs",System.currentTimeMillis()).apply(block)}catch(e:Exception){JSONObject().put("state","UNAVAILABLE").put("reason",e.javaClass.simpleName).put("source",source)}
        val memory=ActivityManager.MemoryInfo().also{(context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)}
        val battery=context.getSystemService(BatteryManager::class.java)
        val power=context.getSystemService(PowerManager::class.java)
        val stat=StatFs(Environment.getDataDirectory().path)
        val result=linkedMapOf<String,JSONObject>()
        result["device"]=observed("Android Build"){put("displayName","${Build.MANUFACTURER} ${Build.MODEL}");put("manufacturer",Build.MANUFACTURER);put("model",Build.MODEL);put("product",Build.PRODUCT);put("device",Build.DEVICE);put("android",Build.VERSION.RELEASE);put("sdk",Build.VERSION.SDK_INT);put("securityPatch",Build.VERSION.SECURITY_PATCH)}
        result["cpu"]=observed("Android Build + java.lang.Runtime"){put("chipset",Build.SOC_MODEL);put("chipsetManufacturer",Build.SOC_MANUFACTURER);put("board",Build.BOARD);put("hardware",Build.HARDWARE);put("logicalProcessors",Runtime.getRuntime().availableProcessors());put("abis",JSONArray(Build.SUPPORTED_ABIS.toList()));put("cpuTemperature","UNAVAILABLE_FROM_AUTHORIZED_PUBLIC_API");put("cpuUtilization","NOT_MEASURED")}
        result["memory"]=observed("ActivityManager.MemoryInfo"){put("totalBytes",memory.totalMem);put("availableBytes",memory.availMem);put("usedBytesDerived",memory.totalMem-memory.availMem);put("lowMemory",memory.lowMemory);put("thresholdBytes",memory.threshold)}
        result["storage"]=observed("StatFs application data volume"){put("totalBytes",stat.totalBytes);put("availableBytes",stat.availableBytes);put("usedBytesDerived",stat.totalBytes-stat.availableBytes);put("scope","APP_DATA_VOLUME_NOT_ALL_EXTERNAL_STORAGE")}
        result["battery"]=observed("BatteryManager + ACTION_BATTERY_CHANGED"){val b=context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED));fun prop(name:String,id:Int){val v=battery.getIntProperty(id);put(name,if(v==Int.MIN_VALUE)"UNSUPPORTED" else v)};prop("capacityPercent",BatteryManager.BATTERY_PROPERTY_CAPACITY);prop("currentMicroamps",BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);prop("chargeMicroampHours",BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);put("charging",battery.isCharging);if(b!=null){put("temperatureCelsius",b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0);put("voltageMillivolts",b.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0));put("healthCode",b.getIntExtra(BatteryManager.EXTRA_HEALTH,0));put("temperatureScope","BATTERY_NOT_CPU")}}
        result["thermal"]=observed("PowerManager"){put("thermalStatus",power.currentThermalStatus);put("powerSaveMode",power.isPowerSaveMode);put("interactive",power.isInteractive);put("cpuTemperature","UNAVAILABLE_FROM_AUTHORIZED_PUBLIC_API")}
        result["display"]=observed("DisplayManager"){val a=JSONArray();context.getSystemService(DisplayManager::class.java).displays.forEach{d->val mode=d.mode;a.put(JSONObject().put("name",d.name).put("displayId",d.displayId).put("widthPixels",mode.physicalWidth).put("heightPixels",mode.physicalHeight).put("refreshRateHz",mode.refreshRate).put("state",d.state))};put("displays",a)}
        result["usb"]=observed("UsbManager connected devices only"){val a=JSONArray();context.getSystemService(UsbManager::class.java).deviceList.values.forEach{d->a.put(JSONObject().put("deviceName",d.deviceName).put("manufacturer",d.manufacturerName?:"UNAVAILABLE").put("product",d.productName?:"UNAVAILABLE").put("vendorId",d.vendorId).put("productId",d.productId).put("deviceClass",d.deviceClass).put("interfaceCount",d.interfaceCount).put("permissionGranted",context.getSystemService(UsbManager::class.java).hasPermission(d)))};put("connectedDevices",a);put("scope","CONNECTED_USB_ONLY_NO_AUTOMATIC_OPEN")}
        result["cameras"]=observed("CameraManager inventory only"){put("cameraIds",JSONArray(context.getSystemService(CameraManager::class.java).cameraIdList.toList()));put("capture","NOT_OPENED")}
        result["audio"]=observed("AudioManager device inventory only"){val a=JSONArray();context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_ALL).forEach{d->a.put(JSONObject().put("name",d.productName.toString()).put("type",d.type).put("input",d.isSource).put("output",d.isSink))};put("devices",a);put("recording","NOT_STARTED");put("selectedVoice","OWNED_BY_LEEWAY_VOICE_NOT_DEVICE_DIAGNOSTICS")}
        result["sensors"]=observed("SensorManager descriptor inventory"){val a=JSONArray();(context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getSensorList(Sensor.TYPE_ALL).forEach{s->a.put(JSONObject().put("name",s.name).put("vendor",s.vendor).put("type",s.stringType))};put("sensors",a);put("streaming","NOT_STARTED")}
        result["network"]=observed("ConnectivityManager capabilities; no scanning"){val cm=context.getSystemService(ConnectivityManager::class.java);val nc=cm.getNetworkCapabilities(cm.activeNetwork);put("connected",nc!=null);put("validatedInternet",nc?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true);put("wifi",nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true);put("cellular",nc?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)==true);put("nearbyDiscovery","NOT_REQUESTED_OWNER_PERMISSION_REQUIRED")}
        result["runtime"]=observed("Android uptime + LeeWay diagnostics contract"){put("uptimeMs",SystemClock.elapsedRealtime());put("firmwareElectricalDiagnostics","NOT_EXPOSED_BY_PUBLIC_API");put("formulaState","NOT_EXECUTED_UNCALIBRATED_DIAGNOSTICS_ADAPTER");put("continuumProjection","DEVICE_LOCAL_OBSERVATIONS_ELIGIBLE_FOR_GOVERNED_PERSISTENCE_NOT_FABRICATED_TELEMETRY")}
        return result
    }
    @Synchronized fun refresh(context:Context,force:Boolean=false){if(!force&&SystemClock.elapsedRealtime()-lastCapture<5000)return;val identity=AndroidDigitalBrainAdapter.identity(context);val sections=capture(context);val now=System.currentTimeMillis();LeeWayBodyDatabases(context).use{dbs->val store=AndroidDigitalBrainAdapter.SqliteBrainStore(dbs.brain.writableDatabase);check(store.owner()==identity){"DIAGNOSTICS_BRAIN_OWNER_MISMATCH"};val root=DigitalBrain.rootId(identity)+":system:hardware";store.atomic{store.upsertNode(BrainNode(root,DigitalBrain.rootId(identity)+":system","universe","${Build.MANUFACTURER} ${Build.MODEL}",mapOf("capturedAtMs" to now.toString(),"source" to "LOCAL_NATIVE_HARDWARE_OBSERVATIONS","state" to "OBSERVED_NOT_FULL_ELECTRICAL_TEST","componentCount" to sections.size.toString())),now);sections.forEach{(key,value)->val metadata=linkedMapOf<String,String>();value.keys().forEach{metadata[it]=value.opt(it).toString()};store.upsertNode(BrainNode(root+":"+key,root,"hardware-component",key.replaceFirstChar{it.uppercase()},metadata),now)};val values=android.content.ContentValues().apply{put("captured_at",now);put("cpu_json",sections["cpu"].toString());put("memory_json",sections["memory"].toString());put("storage_json",sections["storage"].toString());put("battery_json",sections["battery"].toString());put("thermal_json",sections["thermal"].toString());put("display_json",sections["display"].toString());put("sensors_json",sections["sensors"].toString());put("network_json",sections["network"].toString())};dbs.brain.writableDatabase.insertOrThrow("hardware_stats",null,values)}};lastCapture=SystemClock.elapsedRealtime()}
}
