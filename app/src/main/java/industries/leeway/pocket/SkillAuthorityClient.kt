/*
LEEWAY
REGION: POCKET.SKILLS
TAG: POCKET.LEEWAY.SKILL.AUTHORITY.CLIENT
WHAT: Read canonical LeeWay skill instructions from a commit-pinned static discovery index
WHY: Give Pocket Agent real skill context without pretending the undeployed remote MCP executed
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android LeeWay Pocket Agent
WHEN: 2026-09-29
HOW: HTTPS read-only index -> deterministic token match -> immutable raw GitHub SKILL.md fetch -> bounded prompt context
*/
package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class SkillAuthorityClient(private val context: Context) {
    data class SkillContext(
        val promptContext: String,
        val evidence: String,
        val selected: List<String>,
        val sourceCommit: String
    )

    private val prefs=context.getSharedPreferences("leeway-pocket-skills",Context.MODE_PRIVATE)

    fun contextFor(request: String): SkillContext {
        val indexText=fetch(INDEX_URL) ?: prefs.getString("index_json",null).orEmpty()
        if(indexText.isBlank()){
            return SkillContext(
                promptContext="LEEWAY SKILLS: UNAVAILABLE. Do not claim a skill was loaded or executed.",
                evidence="skills=UNAVAILABLE",
                selected=emptyList(),
                sourceCommit=""
            )
        }

        val root=try{JSONObject(indexText)}catch(_:Exception){
            return SkillContext(
                promptContext="LEEWAY SKILLS: INDEX INVALID. Do not claim a skill was loaded or executed.",
                evidence="skills=INDEX_INVALID",
                selected=emptyList(),
                sourceCommit=""
            )
        }
        prefs.edit().putString("index_json",indexText).apply()

        val sourceCommit=root.optString("skillsSourceCommit")
        val rows=root.optJSONArray("skills")
        if(sourceCommit.isBlank() || rows==null){
            return SkillContext(
                promptContext="LEEWAY SKILLS: INDEX INCOMPLETE. Do not claim a skill was loaded or executed.",
                evidence="skills=INDEX_INCOMPLETE",
                selected=emptyList(),
                sourceCommit=sourceCommit
            )
        }

        val tokens=request.lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .filter{it.length>=3}
            .distinct()

        data class Candidate(val slug:String,val path:String,val score:Int)
        val candidates=mutableListOf<Candidate>()
        for(i in 0 until rows.length()){
            val row=rows.optJSONObject(i)?:continue
            val slug=row.optString("slug")
            val path=row.optString("path")
            val haystack=(slug+" "+path).lowercase(Locale.US)
            var score=0
            for(token in tokens){
                if(haystack.contains(token))score+=if(slug.contains(token,true))3 else 1
            }
            if(score>0)candidates+=Candidate(slug,path,score)
        }

        val required=listOf(
            "leeway-continuity-authority",
            "leeway-context-engineering",
            "leeway-formula-governance"
        )
        val chosen=linkedMapOf<String,String>()

        for(slug in required){
            for(i in 0 until rows.length()){
                val row=rows.optJSONObject(i)?:continue
                if(row.optString("slug")==slug){
                    chosen[slug]=row.optString("path")
                    break
                }
            }
        }
        candidates.sortedWith(compareByDescending<Candidate>{it.score}.thenBy{it.path})
            .forEach{
                if(chosen.size<5)chosen.putIfAbsent(it.slug,it.path)
            }

        val loaded=mutableListOf<String>()
        val chunks=mutableListOf<String>()
        var budget=MAX_CONTEXT_CHARS
        for((slug,path) in chosen){
            if(budget<=0)break
            val rawUrl="https://raw.githubusercontent.com/4citeB4U/LeeWay-Agent-Skills/$sourceCommit/$path"
            val skill=fetch(rawUrl) ?: continue
            val take=skill.take(minOf(PER_SKILL_CHARS,budget))
            chunks+="SKILL[$slug] SOURCE=$path@$sourceCommit\n$take"
            loaded+=slug
            budget-=take.length
        }

        if(chunks.isEmpty()){
            return SkillContext(
                promptContext="LEEWAY SKILLS: INDEX RESOLVED BUT NO SKILL DOCUMENT COULD BE FETCHED. Do not claim skill use.",
                evidence="skills=FETCH_BLOCKED source=$sourceCommit",
                selected=emptyList(),
                sourceCommit=sourceCommit
            )
        }

        return SkillContext(
            promptContext=buildString{
                append("LEEWAY SKILL CONTEXT — READ-ONLY AUTHORITY INPUT\n")
                append("These documents guide this turn. Reading them is not tool execution or Formula execution.\n")
                append(chunks.joinToString("\n\n"))
            },
            evidence="skills=CONTEXT_USED source=$sourceCommit selected="+loaded.joinToString(","),
            selected=loaded,
            sourceCommit=sourceCommit
        )
    }

    private fun fetch(url:String):String?=try{
        val connection=(URL(url).openConnection() as HttpURLConnection).apply{
            requestMethod="GET"
            connectTimeout=8000
            readTimeout=12000
            setRequestProperty("User-Agent","LeeWay-Pocket-Agent/0.2")
            setRequestProperty("Cache-Control","no-cache")
        }
        val code=connection.responseCode
        val body=if(code in 200..299)connection.inputStream.bufferedReader().use{it.readText()} else null
        connection.disconnect()
        body
    }catch(_:Exception){null}

    companion object{
        private const val INDEX_URL=
            "https://raw.githubusercontent.com/4citeB4U/LeeWay-Agent-Skills/96ea94dda56e642887e600b54528b69919f0a2e6/docs/pocket-skills-index.json"
        private const val MAX_CONTEXT_CHARS=9000
        private const val PER_SKILL_CHARS=2400
    }
}
