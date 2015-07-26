package com.tjoris.timekeeper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public abstract class ConfirmationDialog extends DialogFragment
{
	private int fMessage;
	private int fPositiveMessage;
	private int fNegativeMessage;
	
	public ConfirmationDialog()
	{
	}
	
	public void setOptions(final int message, final int positiveMessage, final int negativeMessage)
	{
		fMessage = message;
		fPositiveMessage = positiveMessage;
		fNegativeMessage = negativeMessage;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(fMessage).setPositiveButton(fPositiveMessage, new DialogInterface.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int id)
			{
				confirm();
			}
		}).setNegativeButton(fNegativeMessage, null);
		return builder.create();
	}
	
	public abstract void confirm();
}