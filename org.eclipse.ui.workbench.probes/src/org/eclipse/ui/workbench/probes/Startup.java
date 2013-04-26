package org.eclipse.ui.workbench.probes;

import com.yourkit.probes.Probes;

public class Startup implements org.eclipse.ui.IStartup {

	@Override
	public void earlyStartup() {
		Probes.registerProbes(OpenEditorProbe.class);
	}

}
