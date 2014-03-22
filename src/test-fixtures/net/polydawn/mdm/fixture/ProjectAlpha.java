package net.polydawn.mdm.fixture;

import static net.polydawn.mdm.fixture.FixtureUtil.*;
import java.io.*;
import net.polydawn.mdm.test.*;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;
import us.exultant.ahs.iob.*;
import us.exultant.ahs.util.*;

/**
 * ProjectAlpha is a project with no dependencies and one commit (1 file, 1 dir, 1 deep
 * file).
 */
public class ProjectAlpha implements Fixture {
	public ProjectAlpha(String path) throws IOException {
		repo = setUpPlainRepo(path);

		WithCwd wd = new WithCwd(repo.getWorkTree()); {
			IOForge.saveFile("alpha file 1", new File("./alpha").getCanonicalFile());
			new File("./dir/").getCanonicalFile().mkdirs();
			IOForge.saveFile("alpha file 2", new File("./dir/alpha2").getCanonicalFile());
		} wd.close();

		try {
			new Git(repo).add()
				.addFilepattern(".")
				.call();
		} catch (NoFilepatternException e) {
			throw new MajorBug(e); // why would an api throw exceptions like this *checked*?
		} catch (GitAPIException e) {
			throw new MajorBug(e);
		}

		try {
			new Git(repo).commit()
				.setAll(true)
				.setMessage("commit 1 in ProjectAlpha")
				.call();
		} catch (NoMessageException e) {
			throw new MajorBug(e); // why would an api throw exceptions like this *checked*?
		} catch (NoHeadException e) {
			throw new MajorBug(e);
		} catch (UnmergedPathsException e) {
			throw new MajorBug(e);
		} catch (ConcurrentRefUpdateException e) {
			throw new MajorBug(e);
		} catch (WrongRepositoryStateException e) {
			throw new MajorBug(e);
		} catch (GitAPIException e) {
			throw new MajorBug(e);
		}
	}

	Repository repo;

	public Repository getRepo() {
		return repo;
	}
}