package com.tjoris.timekeeper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ConfirmationDialog extends DialogFragment
{
	public interface IListener
	{
		public void confirm();
	}
	
	private final int fMessage;
	private final int fPositiveMessage;
	private final int fNegativeMessage;
	private final IListener fListener;
	
	public ConfirmationDialog(final int message, final int positiveMessage, final int negativeMessage, final IListener listener)
	{
		fMessage = message;
		fPositiveMessage = positiveMessage;
		fNegativeMessage = negativeMessage;
		fListener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(fMessage).setPositiveButton(fPositiveMessage, new DialogInterface.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int id)
			{
				fListener.confirm();
			}
		}).setNegativeButton(fNegativeMessage, null);
		return builder.create();
	}
}