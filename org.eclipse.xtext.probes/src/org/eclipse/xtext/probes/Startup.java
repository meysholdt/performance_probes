package org.eclipse.xtext.probes;

import com.google.inject.probes.InjectorProbe;
import com.yourkit.probes.Probes;

public class Startup implements org.eclipse.ui.IStartup {

	@Override
	public void earlyStartup() {
		Probes.registerProbes(InjectorProbe.class);
	}

}
