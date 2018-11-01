package nopmd;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.swt.widgets.Display;

public class CommandUtil {
	public static String run(List<String> commandList, File workingDir) {
		try {
			ProcessBuilder builder = null;
			StringBuffer result = new StringBuffer();
			// the current executable
			builder = new ProcessBuilder(commandList);
			builder.directory(workingDir);
			Process p = builder.start();
			java.io.InputStream is = p.getInputStream();
			java.io.BufferedReader reader = new java.io.BufferedReader(
					new InputStreamReader(is));
			// And print each line
			String s = null;
			while ((s = reader.readLine()) != null) {
				result.append(s).append("\r\n"); //$NON-NLS-1$
			}
			is.close();
			reader = new java.io.BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			// And print each line
			while ((s = reader.readLine()) != null) {
				result.append(s).append("\r\n"); //$NON-NLS-1$
			}
			is.close();
			return result.toString();
		} catch (final IOException e) {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					Handler.log(e);
				}
			});
		}
		return null;
	}
}
