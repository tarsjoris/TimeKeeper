package com.tjoris.timekeeper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public abstract class InputDialog extends DialogFragment
{
	private final int fTitleID;
	private final int fViewID;
	private final int fPositiveTextID;
	private final int fNegativetextID;
	
	public InputDialog(final int titleID, final int viewID, final int positiveTextID, final int negativeTextID)
	{
		fTitleID = titleID;
		fViewID = viewID;
		fPositiveTextID = positiveTextID;
		fNegativetextID = negativeTextID;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(fTitleID).setView(fViewID).setPositiveButton(fPositiveTextID, new DialogInterface.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int id)
			{
				dialogConfirmed(getDialog());
			}
		}).setNegativeButton(fNegativetextID, null);
		return builder.create();
	}
	
	public abstract void dialogConfirmed(Dialog dialog);
}
