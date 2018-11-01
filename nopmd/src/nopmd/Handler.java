package nopmd;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Enable/disable PMD builder globally for all projects
 *
 */
@SuppressWarnings("restriction")
public class Handler implements IHandler {

	private static final String PLUGIN_ID = "nopmd";

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}
	static Map<BuildCommand,Object> originalBuilders = new HashMap<>();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean pmdDisabled=false;
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (project.isAccessible()) {
				ICommand[] commands = ((Project) project).internalGetDescription().getBuildSpec(false);
				for (ICommand command : commands) {
					if (command instanceof BuildCommand) {
						BuildCommand bc = (BuildCommand) command;
						String name = bc.getName();
						if (name!=null) {
							name = name.toLowerCase(Locale.ENGLISH);
							if (name.contains("pmd")) {
								// this is the pmd builder - disable it now
								Object ori = bc.getBuilders();
								if (ori instanceof NullBuilder) {
									Object builders = originalBuilders.remove(bc);
									bc.setBuilders(builders);
									pmdDisabled=false;
								}else {
									originalBuilders.put(bc, ori);
									bc.setBuilders(new NullBuilder());
									pmdDisabled=true;
								}
							}
						}
						
						
					}
				}
			}
		}
		if (pmdDisabled==true)
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info", "PMD builders are now disabled");
		else
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info", "PMD builders are now enabled");
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
	
	public static void log(Throwable e) {
		Bundle bundle = FrameworkUtil.getBundle(Handler.class);
		if (bundle != null) {
			Platform.getLog(bundle).log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(),e));
		} else { //fallback when plugin is not running
			e.printStackTrace();
		}
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
	
	private static class NullBuilder extends IncrementalProjectBuilder {
		public NullBuilder() {
			try {
				Field f = InternalBuilder.class.getDeclaredField("command");
				f.setAccessible(true);
				f.set(this, new BuildCommand());
			} catch (Exception e) {
				log(e);
			}
		}

		@Override
		protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
			return null;
		}
	}
}
