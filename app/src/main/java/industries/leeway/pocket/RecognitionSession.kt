package industries.leeway.pocket

/** Cancellation invalidates callbacks already queued by Android's recognition service. */
class RecognitionSession {
    private var generation=0
    private var typing=false
    fun begin():Int? {
        if(typing)return null
        return ++generation
    }
    fun cancel(){generation++}
    fun beginTyping(){typing=true;cancel()}
    fun endTyping(){typing=false}
    fun accepts(token:Int)=!typing && token==generation
}
