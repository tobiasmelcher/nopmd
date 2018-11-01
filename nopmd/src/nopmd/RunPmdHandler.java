package nopmd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * loops over all projects and runs "git status" on all project roots; collects
 * all modified files and runs then pmd only on the modified files.
 *
 */
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
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				Set<String> modifiedFiles = new HashSet<>();
				for (IProject project : projects) {
					if (project.isAccessible() == false)
						continue;
					IPath loc = project.getLocation();
					if (loc == null)
						continue;
					String path = loc.toOSString();
					if (path == null || path.length() == 0)
						continue;
					getModifiedFiles(path, modifiedFiles);
				}
				if (modifiedFiles.size() == 0)
					return Status.OK_STATUS;
				runPmd(modifiedFiles);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

	private static void getModifiedFiles(String gitRoot, Set<String> result) {
		String res = CommandUtil.run(Arrays.asList("git", "status"), new File(gitRoot));
		if (res == null || res.length() == 0)
			return;
		Matcher m = Pattern.compile("modified:([^\\r\\n]*)").matcher(res);
		while (m.find()) {
			String line = m.group(1).trim();
			String p = gitRoot + "/" + line;
			File f = new File(p);
			if (f.exists()) {
				try {
					p = f.getCanonicalPath();
					result.add(p);
				} catch (IOException e) {
					Handler.log(e);
				}
			}
		}
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
