package com.tjoris.timekeeper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public abstract class InputDialog extends DialogFragment
{
	private final LayoutInflater fLayoutInflater;
	private final int fTitleID;
	private final int fIconID;
	private final int fViewID;
	private final int fPositiveTextID;
	private final int fNegativetextID;
	
	public InputDialog(final LayoutInflater layoutInflater, final int titleID, final int iconID, final int viewID, final int positiveTextID, final int negativeTextID)
	{
		fLayoutInflater = layoutInflater;
		fTitleID = titleID;
		fIconID = iconID;
		fViewID = viewID;
		fPositiveTextID = positiveTextID;
		fNegativetextID = negativeTextID;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final View view = fLayoutInflater.inflate(fViewID, null);
		viewCreated(view);
		builder.setTitle(fTitleID).setIcon(fIconID).setView(view).setPositiveButton(fPositiveTextID, new DialogInterface.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int id)
			{
				dialogConfirmed(getDialog());
			}
		}).setNegativeButton(fNegativetextID, null);
		return builder.create();
	}
	
	public void viewCreated(final View view)
	{
	}
	
	public abstract void dialogConfirmed(Dialog dialog);
}
