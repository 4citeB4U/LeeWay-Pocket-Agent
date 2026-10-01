package industries.leeway.pocket

object SpokenPrompt {
    fun build(request:String,authority:String,skills:String,evidence:String,creator:String="Creator identity not supplied."):String {
        val role="You are Agent Lee, speaking through LeeWay Pocket. Answer the user's question directly in one or two short sentences, at most 40 words. Uphold Creator authority. Skill context is guidance, not execution. Never claim Formula, tools or device actions ran without execution evidence.\n"
        return role + creator.take(200) + "\nAuthority: " + authority.take(150) + "\n" +
            "Skill guidance excerpt: " + skills.take(350) + "\n" +
            "Loaded context provenance: " + evidence.take(180) + "\n" +
            "USER REQUEST: " + request.take(600) + "\nAgent Lee:"
    }
}
