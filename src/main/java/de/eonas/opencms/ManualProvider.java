package de.eonas.opencms;

import org.opencms.workplace.editors.directedit.CmsDirectEditDefaultProvider;
import org.opencms.workplace.editors.directedit.CmsDirectEditMode;

@SuppressWarnings("UnusedDeclaration")
public class ManualProvider extends CmsDirectEditDefaultProvider {
	public boolean isManual(CmsDirectEditMode mode) {
		return (mode == CmsDirectEditMode.MANUAL);
	}
}
