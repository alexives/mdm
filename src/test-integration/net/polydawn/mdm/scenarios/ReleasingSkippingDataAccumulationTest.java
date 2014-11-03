package net.polydawn.mdm.scenarios;

import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import net.polydawn.mdm.*;
import net.polydawn.mdm.fixture.*;
import net.polydawn.mdm.test.*;
import net.polydawn.mdm.test.WithCwd;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.junit.*;
import org.junit.runner.*;
import us.exultant.ahs.iob.*;

@RunWith(OrderedJUnit4ClassRunner.class)
public class ReleasingSkippingDataAccumulationTest extends TestCaseUsingRepository {
	@Test
	public void releasing_in_a_fresh_init_linked_without_accumulation() throws Exception {
		Fixture projectAlpha = new ProjectAlpha("projectAlpha");

		// initialize a new release repo locally and make a release into it
		WithCwd wd = new WithCwd(projectAlpha.getRepo().getWorkTree()); {
			assertJoy(Mdm.run(
				"release-init",
				"--use-defaults"
			));

			IOForge.saveFile("placeholder", new File("whatever").getCanonicalFile());

			assertJoy(Mdm.run(
				"release",
				"--version=v1",
				"--files=whatever",
				"--skip-accumulation"
			));
		} wd.close();

		// assert on the refs in the release module we added to the project repo
		Repository releaseRepo = new RepositoryBuilder()
			.setWorkTree(new File("projectAlpha/releases/").getCanonicalFile())
			.build();
		Collection<Ref> refs = new Git(releaseRepo).lsRemote()
				.setRemote(releaseRepo.getWorkTree().toString())
				.call();
		List<String> refNames = new ArrayList<String>(refs.size());
		for (Ref r : refs) refNames.add(r.getName());
		assertTrue("head ref present in release module", refNames.contains("HEAD"));
		assertTrue("master ref present in release module", refNames.contains("refs/heads/master"));
		assertTrue("mdm/init ref present in release module", refNames.contains("refs/heads/mdm/init"));
		assertTrue("release branch present in release module", refNames.contains("refs/heads/mdm/release/v1"));
		assertTrue("release tag present in release module", refNames.contains("refs/tags/release/v1"));
		assertTrue("accumlation tag present in release module", refNames.contains("refs/tags/mdm/master/v1"));
		assertEquals("exactly these refs present in release module", 6, refNames.size());

		// assert on the files in the master commit
		List<String> masterFiles = Arrays.asList(new File("projectAlpha/releases/").getCanonicalFile().list());
		assertTrue("readme file remains present in master commit", masterFiles.contains("README"));
		assertTrue("version dir *not* present in master commit", !masterFiles.contains("v1"));
		assertTrue("git data dir still here", masterFiles.contains(".git")); // this isn't of interest per se, it's just to make sure the contents of this list are all accounted for
		assertEquals("exactly these files present in master commit", 2, masterFiles.size());

		// assert on the files in the release commit
		new Git(releaseRepo).checkout()
			.setName("refs/tags/release/v1")
			.call();
		List<String> releaseFiles = Arrays.asList(new File("projectAlpha/releases/").getCanonicalFile().list());
		assertTrue("release file present in release commit", releaseFiles.contains("whatever"));
		assertTrue("git data dir still here", masterFiles.contains(".git")); // this isn't of interest per se, it's just to make sure the contents of this list are all accounted for
		assertEquals("exactly these files present in release commit", 2, releaseFiles.size());
	}
}
