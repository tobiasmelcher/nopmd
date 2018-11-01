package nopmd;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

/**
 * loops over all projects and runs "git status" on all project roots; collects
 * all modified files and runs then pmd only on the modified files.
 *
 */
@SuppressWarnings("restriction")
public class RunPmdHandler implements IHandler {

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("run pmd on all modified git files") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					Activator act = Activator.getDefault();
					RepositoryCache cache = act.getRepositoryCache();
					Repository[] repositories = cache.getAllRepositories();
					if (repositories == null || repositories.length == 0) {
						msg("no GIT repositories found - do not run pmd on all uncommited changes");
						return Status.OK_STATUS;
					}
					Set<String> modifiedFiles = new HashSet<>();
					for (Repository repo : repositories) {
						Git git = new Git(repo);
						try {
							org.eclipse.jgit.api.Status status = git.status().call();
							Set<String> uncommittedChanges = status.getUncommittedChanges();
							if (uncommittedChanges == null || uncommittedChanges.size() == 0)
								continue;
							for (String change : uncommittedChanges) {
								File f = new File(repo.getWorkTree() + "/" + change);
								if (f.exists()) {
									modifiedFiles.add(f.getCanonicalPath());
								}
							}
						} finally {
							git.close();
						}
					}
					runPmd(modifiedFiles);
					msg("PMD executed on "+modifiedFiles.size()+" modified files");
				} catch (Exception e) {
					Handler.log(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

	private void msg(final String msg) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "PMD", msg);
			}
		});
	}

	private static void runPmd(Set<String> allFiles) {
		try {
			for (String file : allFiles) {
				@SuppressWarnings("deprecation")
				IFile[] r = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(file));
				if (r != null) {
					for (IFile c : r) {
						if (c instanceof IFile) {
							IFile f = (IFile) c;
							runPmdCheckAction(f);
						}
					}
				}
			}
		} catch (Throwable e1) {
			Handler.log(e1);
		}
	}

	private static Method reviewSingleResourceMethod;
	private static Object pmdCheckAction;

	private static void runPmdCheckAction(IFile f) {
		try {
			if (pmdCheckAction == null) {
				Bundle bundle = Platform.getBundle("ch.acanda.eclipse.pmd.core");
				if (bundle.getSymbolicName().contains(".pmd")) {
					Class<?> clazz = bundle.loadClass("ch.acanda.eclipse.pmd.builder.PMDBuilder");
					pmdCheckAction = clazz.newInstance();
					reviewSingleResourceMethod = clazz.getDeclaredMethod("analyze", IResource.class, boolean.class,
							IProgressMonitor.class);
					reviewSingleResourceMethod.setAccessible(true);
				}
			}
			reviewSingleResourceMethod.invoke(pmdCheckAction, f, true, new NullProgressMonitor());
		} catch (Throwable e1) {
			Handler.log(e1);
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
}
