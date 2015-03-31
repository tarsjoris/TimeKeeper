package com.tjoris.timekeeper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class PlaylistNameDialog extends DialogFragment
{
	public interface IListener
	{
		public void add(CharSequence name);
	}
	
	private final IListener fListener;
	
	public PlaylistNameDialog(final IListener listener)
	{
		fListener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.overview_action_addplaylist).setView(R.layout.dialog_addplaylist).setPositiveButton(R.string.overview_addplaylist_add, new DialogInterface.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int id)
			{
				fListener.add(((EditText)getDialog().findViewById(R.id.overiew_addplaylist_name)).getText());
			}
		}).setNegativeButton(R.string.overview_addplaylist_cancel, null);
		return builder.create();
	}
}
