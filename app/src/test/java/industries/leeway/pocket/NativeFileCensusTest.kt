/*
REGION: LEEWAY.BRAIN.NATIVE.QUALIFICATION
TAG: REAL_FILESYSTEM_METADATA_SCANNER_TESTS
WHO: LeeWay qualification; WHAT: Execute the production scanner on real temporary host files.
WHEN: Before Android qualification; WHERE: JVM host, not a physical Android claim.
WHY: Reading metadata and qualifying a complete authoritative census are separate results.
HOW: Real create/edit/rename/delete/link inputs; identity faults are explicitly labelled test fixtures.
LICENSE: MIT
*/
package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

class NativeFileCensusTest {
    @get:Rule val temp=TemporaryFolder()
    private fun root()=temp.newFolder().toPath()
    private fun write(path:Path,text:String):Path {Files.createDirectories(path.parent);return Files.write(path,text.toByteArray(Charsets.UTF_8))}
    private fun scan(path:Path)=NativeFileCensus.collect(path,emptySet())
    private fun assertNativeIdentityBoundary(path:Path,census:NativeCensus) {
        val key=Files.readAttributes(path,BasicFileAttributes::class.java,LinkOption.NOFOLLOW_LINKS).fileKey()
        val expected=if(key==null)listOf("ROOT_OBJECT_IDENTITY_UNAVAILABLE")else emptyList()
        assertEquals("Unknown native identity MUST keep absence-based deletion blocked",expected,census.omissions)
    }

    @Test fun realHierarchyHasStableParentKeys(){val r=root();write(r.resolve("folder/note.txt"),"hello");val c=scan(r);assertNativeIdentityBoundary(r,c);val d=c.entries.single{it.directory};val f=c.entries.single{!it.directory};assertEquals(d.objectKey,f.parentKey);assertEquals(2,c.directories.size);assertEquals(5L,f.sizeBytes)}
    @Test fun changedMetadataProducesChangedVersion(){val r=root();val file=write(r.resolve("note.txt"),"a");val first=scan(r).entries.single();write(file,"longer");Files.setLastModifiedTime(file,FileTime.fromMillis(first.sourceModifiedAtMs!!+2000));val second=scan(r).entries.single();assertEquals(first.objectKey,second.objectKey);assertNotEquals(first.metadataVersion,second.metadataVersion);assertEquals(6L,second.sizeBytes)}
    @Test fun removalStillRequiresKnownRootIdentityForAbsenceAdmission(){val r=root();val file=write(r.resolve("gone.txt"),"data");assertEquals(1,scan(r).entries.size);Files.delete(file);val c=scan(r);assertTrue(c.entries.isEmpty());assertNativeIdentityBoundary(r,c)}
    @Test fun renameIsNotInferredFromNameOrContentSimilarity(){val r=root();val a=write(r.resolve("old.txt"),"same bytes");val old=scan(r).entries.single();Files.move(a,r.resolve("new.txt"));val fresh=scan(r).entries.single();assertNotEquals(old.objectKey,fresh.objectKey);assertEquals("new.txt",fresh.title)}
    @Test fun relocationDoesNotCompilePhysicalRootIntoObjectKey(){val one=root();val two=root();val a=write(one.resolve("nested/file.txt"),"same");val b=write(two.resolve("nested/file.txt"),"same");Files.setLastModifiedTime(b,Files.getLastModifiedTime(a));val x=scan(one).entries.single{!it.directory};val y=scan(two).entries.single{!it.directory};assertEquals(x.objectKey,y.objectKey);assertNotEquals(x.sourceUri,y.sourceUri);assertEquals(x.metadataVersion,y.metadataVersion)}
    @Test fun protectedBrainOutputsAreNotReingested(){val r=root();write(r.resolve("owned-state/brain.db"),"not an input");write(r.resolve("note.txt"),"input");val c=NativeFileCensus.collect(r,setOf(r.resolve("owned-state")));assertEquals(listOf("note.txt"),c.entries.map{it.title});assertNativeIdentityBoundary(r,c)}
    @Test fun aProtectedRootCannotPretendToBeEmpty(){val r=root();write(r.resolve("note.txt"),"input");assertThrows(IllegalStateException::class.java){NativeFileCensus.collect(r,setOf(r))}}
    @Test fun missingRootIsAnErrorNotAnEmptyCensus(){val r=root().resolve("missing");assertThrows(java.io.IOException::class.java){scan(r)}}
    @Test fun budgetExceededIsReportedAsIncomplete(){val r=root();repeat(5){write(r.resolve("f$it.txt"),"input")};val c=NativeFileCensus.collect(r,emptySet(),maxEntries=2);assertEquals(2,c.entries.size);assertTrue(c.omissions.contains("SCAN_BUDGET_REACHED"))}
    @Test fun depthLimitIsNotACompleteDirectoryClaim(){val r=root();write(r.resolve("a/b/c.txt"),"input");val c=NativeFileCensus.collect(r,emptySet(),maxDepth=1);assertTrue(c.omissions.contains("SCAN_DEPTH_LIMIT"));assertFalse(c.entries.any{it.title=="c.txt"})}
    @Test fun unicodeAndSpacesRemainObservations(){val r=root();write(r.resolve("Folder with spaces/資料.txt"),"input");val c=scan(r);assertTrue(c.entries.any{it.title=="資料.txt"});assertTrue(c.entries.single{!it.directory}.sourceUri.contains("%"));assertNativeIdentityBoundary(r,c)}
    @Test fun directoryLinkCannotEscapeAuthorizedRoot(){val r=root();val outside=root();write(outside.resolve("outside.txt"),"private-test-input");val link=r.resolve("escape")
        try {
            if(System.getProperty("os.name").orEmpty().startsWith("Windows")){
                val process=ProcessBuilder("cmd.exe","/d","/c","mklink","/J",link.toString(),outside.toString()).redirectErrorStream(true).start()
                val text=process.inputStream.bufferedReader().use{it.readText()};assertEquals(text,0,process.waitFor())
            }else Files.createSymbolicLink(link,outside)
            val c=scan(r);assertFalse(c.entries.any{it.title=="outside.txt"});assertTrue(c.omissions.contains("UNSUPPORTED_OR_ESCAPING_LINK"))
        } finally {Files.deleteIfExists(link)}
    }
    @Test fun missingNativeIdentityExplicitlyPreventsCompleteCensus(){val r=root();write(r.resolve("note.txt"),"data");val c=NativeFileCensus.collect(r,emptySet(),readRootIdentity={null});assertEquals(listOf("ROOT_OBJECT_IDENTITY_UNAVAILABLE"),c.omissions);assertEquals(1,c.entries.size)}
    @Test fun changedRootIdentityFixtureFailsBeforeCommit(){val r=root();var calls=0;assertThrows(IllegalStateException::class.java){NativeFileCensus.collect(r,emptySet(),readRootIdentity={"TEST_ONLY_ID_"+(calls++)})};assertEquals(2,calls)}
    @Test fun stableRootIdentityFixtureExercisesCompleteCensusLogicOnly(){val r=root();write(r.resolve("note.txt"),"data");var calls=0;val c=NativeFileCensus.collect(r,emptySet(),readRootIdentity={calls++;"TEST_FIXTURE_NOT_PLATFORM_IDENTITY"});assertEquals(2,calls);assertTrue(c.omissions.isEmpty());assertEquals(1,c.entries.size)}
}
