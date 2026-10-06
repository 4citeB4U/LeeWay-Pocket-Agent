/*
REGION: LEEWAY.BRAIN.ADAPTER.FILESYSTEM
TAG: BOUNDED_NOFOLLOW_METADATA_CENSUS
WHO: Brain-authorized native adapter; WHAT: Read filesystem metadata for one resolved root.
WHEN: Reconciliation; WHERE: native boundary, never the shared Brain's logical authority.
WHY: Exercise the same production scanner with real temporary files before device qualification.
HOW: No-follow traversal, explicit exclusions, bounded work, root-identity recheck, no content reads.
LICENSE: MIT
*/
package industries.leeway.pocket

import industries.leeway.brain.FileObservation
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

internal data class NativeCensus(val entries: List<FileObservation>,val omissions: List<String>,val directories: List<File>)

internal object NativeFileCensus {
    private fun sha(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun collect(requestedRoot: Path,excluded: Set<Path>,maxEntries: Int=10000,maxDepth: Int=64,maxMillis: Long=20000,readRootIdentity:(Path)->String?={ path -> Files.readAttributes(path,BasicFileAttributes::class.java,LinkOption.NOFOLLOW_LINKS).fileKey()?.toString() }): NativeCensus {
        require(maxEntries>0 && maxDepth>0 && maxMillis>0) { "CENSUS_BUDGET_INVALID" }
        check(!Files.isSymbolicLink(requestedRoot)) { "INGESTION_ROOT_LINK_REJECTED" }
        val root=requestedRoot.toRealPath()
        check(Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)) { "INGESTION_DIRECTORY_REQUIRED" }
        val protected=excluded.map { it.toAbsolutePath().normalize() }.toSet()
        check(protected.none { root.startsWith(it) }) { "INGESTION_ROOT_IS_PROTECTED" }
        val before=readRootIdentity(root)
        val entries=mutableListOf<FileObservation>();val omissions=mutableListOf<String>();val watches=mutableListOf<File>()
        val began=System.nanoTime()
        fun omitted(reason: String) { if(reason !in omissions) omissions.add(reason) }
        if(before==null)omitted("ROOT_OBJECT_IDENTITY_UNAVAILABLE")
        fun key(path: Path)=sha(root.relativize(path).toString().replace(File.separatorChar,'/'))
        fun add(path: Path,attrs: BasicFileAttributes) {
            val size=if(attrs.isDirectory)null else attrs.size()
            val modified=attrs.lastModifiedTime().toMillis().takeIf { it>=0 }
            entries.add(FileObservation(key(path),if(path.parent==root)null else key(path.parent),path.fileName.toString(),
                attrs.isDirectory,path.toUri().toString(),sha(listOf(attrs.isDirectory,size,modified).joinToString("|")),size,modified))
        }
        Files.walkFileTree(root,emptySet(),maxDepth,object:SimpleFileVisitor<Path>() {
            private fun limit()=entries.size>=maxEntries || (System.nanoTime()-began)/1000000>maxMillis
            override fun preVisitDirectory(dir: Path,attrs: BasicFileAttributes): FileVisitResult {
                if(limit()){omitted("SCAN_BUDGET_REACHED");return FileVisitResult.TERMINATE}
                if(protected.any { dir.startsWith(it) })return FileVisitResult.SKIP_SUBTREE
                if(Files.isSymbolicLink(dir)||!dir.toRealPath().startsWith(root)){omitted("UNSUPPORTED_OR_ESCAPING_LINK");return FileVisitResult.SKIP_SUBTREE}
                watches.add(dir.toFile());if(dir!=root)add(dir,attrs)
                return FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path,attrs: BasicFileAttributes): FileVisitResult {
                if(limit()){omitted("SCAN_BUDGET_REACHED");return FileVisitResult.TERMINATE}
                if(protected.any { file.startsWith(it) })return FileVisitResult.CONTINUE
                if(attrs.isSymbolicLink||Files.isSymbolicLink(file)||!file.toRealPath().startsWith(root)){omitted("UNSUPPORTED_OR_ESCAPING_LINK");return FileVisitResult.CONTINUE}
                if(attrs.isDirectory){omitted("SCAN_DEPTH_LIMIT");return FileVisitResult.CONTINUE}
                if(attrs.isRegularFile)add(file,attrs)else omitted("UNSUPPORTED_RESOURCE_KIND")
                return FileVisitResult.CONTINUE
            }
            override fun visitFileFailed(file: Path,error: java.io.IOException): FileVisitResult {omitted("RESOURCE_UNAVAILABLE_DURING_SCAN");return FileVisitResult.CONTINUE}
            override fun postVisitDirectory(dir: Path,error: java.io.IOException?): FileVisitResult {if(error!=null)omitted("DIRECTORY_ENUMERATION_FAILED");return FileVisitResult.CONTINUE}
        })
        check(!Files.isSymbolicLink(requestedRoot) && requestedRoot.toRealPath()==root && Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)) { "INGESTION_ROOT_CHANGED" }
        val after=readRootIdentity(root)
        check(before==null || before==after) { "INGESTION_ROOT_IDENTITY_CHANGED" }
        return NativeCensus(entries,omissions,watches)
    }
}
