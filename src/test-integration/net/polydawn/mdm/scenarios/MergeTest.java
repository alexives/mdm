package net.polydawn.mdm.scenarios;

import static org.junit.Assert.assertTrue;
import java.io.*;
import net.polydawn.josh.*;
import net.polydawn.mdm.*;
import net.polydawn.mdm.fixture.*;
import net.polydawn.mdm.test.*;
import net.polydawn.mdm.test.WithCwd;
import org.junit.*;
import org.junit.runner.*;
import us.exultant.ahs.iob.*;

@RunWith(OrderedJUnit4ClassRunner.class)
public class MergeTest extends TestCaseUsingRepository {
	final Josh git = new Josh("git");

	Fixture project;
	Fixture releases;

	public void setup() throws Exception {
		project = new ProjectAlpha("projectAlpha");
		releases = new ProjectBetaReleases("projectBeta-releases");

		// set up a library, then two branches with divering versions of it
		WithCwd wd = new WithCwd(project.getRepo().getWorkTree()); {
			assertJoy(Mdm.run(
				"add",
				releases.getRepo().getWorkTree().toString(),
				"--version=v1.0",
				"--name=beta"
			));
			git.args("checkout", "-b", "blue").start().get();
			git.args("checkout", "-b", "green").start().get();

			git.args("checkout", "blue").start().get();
			assertJoy(Mdm.run(
				"alter",
				"lib/beta",
				"--version=v1.1"
			));

			git.args("checkout", "green").start().get();
			assertJoy(Mdm.run(
				"alter",
				"lib/beta",
				"--version=v2.0"
			));

			git.args("checkout", "master").start().get();
			assertJoy(Mdm.run("update"));
		} wd.close();
	}

	@Test
	public void mergeTakingTheirs() throws Exception {
		setup();

		// do a merges.  the second should fail with conflicts.
		WithCwd wd = new WithCwd(project.getRepo().getWorkTree()); {
			// merge one branch.  should go clean.
			git.args("merge", "--no-ff", "blue").start().get();

			// merge second branch.  should conflict (exit code is nonzero)
			git.args("merge", "--no-ff", "green").okExit(1).start().get();

			// choose their gitmodules file, then update to put that version in place
			git.args("checkout", "--theirs", ".gitmodules").start().get();
			assertJoy(Mdm.run("update", "--strict"));

			// should be able to stage changes and commit
			git.args("add", ".gitmodules", "lib/beta").start().get();
			git.args("commit", "--no-edit").start().get();
		} wd.close();

		// now verify.
		File depWorkTreePath = new File(project.getRepo().getWorkTree()+"/lib/beta").getCanonicalFile();
		File depGitDataPath = new File(project.getRepo().getDirectory()+"/modules/lib/beta").getCanonicalFile();

		// i do hope there's a filesystem there now
		assertTrue("dependency module path exists on fs", depWorkTreePath.exists());
		assertTrue("dependency module path is dir", depWorkTreePath.isDirectory());

		// check that anyone else can read this state with a straight face; status should be clean
		new Josh("git").args("status").cwd(project.getRepo().getWorkTree())/*.opts(Opts.NullIO)*/.start().get();
		new Josh("git").args("status").cwd(depWorkTreePath)/*.opts(Opts.NullIO)*/.start().get();
	}

	@Test
	public void mergeTakingOurs() throws Exception {
		setup();

		// do a merges.  the second should fail with conflicts.
		WithCwd wd = new WithCwd(project.getRepo().getWorkTree()); {
			// merge one branch.  should go clean.
			git.args("merge", "--no-ff", "blue").start().get();

			// merge second branch.  should conflict (exit code is nonzero)
			git.args("merge", "--no-ff", "green").okExit(1).start().get();

			// choose their gitmodules file, then update to put that version in place
			git.args("checkout", "--ours", ".gitmodules").start().get();
			assertJoy(Mdm.run("update", "--strict"));

			// should be able to stage changes and commit
			git.args("add", ".gitmodules", "lib/beta").start().get();
			git.args("commit", "--no-edit").start().get();
		} wd.close();

		// now verify.
		File depWorkTreePath = new File(project.getRepo().getWorkTree()+"/lib/beta").getCanonicalFile();
		File depGitDataPath = new File(project.getRepo().getDirectory()+"/modules/lib/beta").getCanonicalFile();

		// i do hope there's a filesystem there now
		assertTrue("dependency module path exists on fs", depWorkTreePath.exists());
		assertTrue("dependency module path is dir", depWorkTreePath.isDirectory());

		// check that anyone else can read this state with a straight face; status should be clean
		new Josh("git").args("status").cwd(project.getRepo().getWorkTree())/*.opts(Opts.NullIO)*/.start().get();
		new Josh("git").args("status").cwd(depWorkTreePath)/*.opts(Opts.NullIO)*/.start().get();
	}

	/**
	 * Tosses out the dependency checkout in the working tree right before attempting
	 * the usual resolve steps. This is to check that mdm-update correctly fetches and
	 * drops things in place on disk even when run in a repo that's in the merging
	 * state.
	 */
	@Test
	public void mergeWithMissingDependencyWorkTree() throws Exception {
		setup();

		// do a merges.  the second should fail with conflicts.
		WithCwd wd = new WithCwd(project.getRepo().getWorkTree()); {
			// merge one branch.  should go clean.
			git.args("merge", "--no-ff", "blue").start().get();

			// screw with the working tree, because fuck you that's why
			IOForge.delete(new File("./lib/beta").getCanonicalFile());

			// merge second branch.  should conflict (exit code is nonzero)
			git.args("merge", "--no-ff", "green").okExit(1).start().get();

			// choose their gitmodules file, then update to put that version in place
			git.args("checkout", "--theirs", ".gitmodules").start().get();
			assertJoy(Mdm.run("update", "--strict"));

			// should be able to stage changes and commit
			git.args("add", ".gitmodules", "lib/beta").start().get();
			git.args("commit", "--no-edit").start().get();
		} wd.close();

		// now verify.
		File depWorkTreePath = new File(project.getRepo().getWorkTree()+"/lib/beta").getCanonicalFile();
		File depGitDataPath = new File(project.getRepo().getDirectory()+"/modules/lib/beta").getCanonicalFile();

		// i do hope there's a filesystem there now
		assertTrue("dependency module path exists on fs", depWorkTreePath.exists());
		assertTrue("dependency module path is dir", depWorkTreePath.isDirectory());

		// check that anyone else can read this state with a straight face; status should be clean
		new Josh("git").args("status").cwd(project.getRepo().getWorkTree())/*.opts(Opts.NullIO)*/.start().get();
		new Josh("git").args("status").cwd(depWorkTreePath)/*.opts(Opts.NullIO)*/.start().get();
	}
}
