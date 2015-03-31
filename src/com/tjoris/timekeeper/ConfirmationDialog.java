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
	private final IListener fListener;
	
	public ConfirmationDialog(final int message, final IListener listener)
	{
		fMessage = message;
		fListener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(fMessage).setPositiveButton(R.string.overview_delete_yes, new DialogInterface.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int id)
			{
				fListener.confirm();
			}
		}).setNegativeButton(R.string.overview_delete_no, null);
		return builder.create();
	}
}