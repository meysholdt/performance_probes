package org.eclipse.xtext.builder.probes;

import org.eclipse.ui.IStartup;

import com.yourkit.probes.Probes;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		Probes.registerProbes(BuilderProbe.class);
	}

}
