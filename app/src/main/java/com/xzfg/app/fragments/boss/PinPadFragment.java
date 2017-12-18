package com.xzfg.app.fragments.boss;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


public class PinPadFragment extends BossModeFragment implements OnClickListener {
  private View pinRing1View;
  private View pinRing2View;
  private View pinRing3View;
  private View pinRing4View;
  private TextView messageView;

  private String pin = "";

  public PinPadFragment() {
  }

  @Override
  public int getType() {
    return Givens.BOSSMODE_TYPE_PINPAD;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (BuildConfig.DEBUG) {
      Timber.w("CREATING PINPAD UI");
    }

    View view = inflater.inflate(R.layout.fragment_boss_mode_pinpad, container, false);
    pinRing1View = view.findViewById(R.id.pin_ring1);
    pinRing2View = view.findViewById(R.id.pin_ring2);
    pinRing3View = view.findViewById(R.id.pin_ring3);
    pinRing4View = view.findViewById(R.id.pin_ring4);
    messageView = (TextView)view.findViewById(R.id.message);
    messageView.setVisibility(!isJdar ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.delete).setOnClickListener(this);
    view.findViewById(R.id.pad0).setOnClickListener(this);
    view.findViewById(R.id.pad1).setOnClickListener(this);
    view.findViewById(R.id.pad2).setOnClickListener(this);
    view.findViewById(R.id.pad3).setOnClickListener(this);
    view.findViewById(R.id.pad4).setOnClickListener(this);
    view.findViewById(R.id.pad5).setOnClickListener(this);
    view.findViewById(R.id.pad6).setOnClickListener(this);
    view.findViewById(R.id.pad7).setOnClickListener(this);
    view.findViewById(R.id.pad8).setOnClickListener(this);
    view.findViewById(R.id.pad9).setOnClickListener(this);

    if (BuildConfig.DEBUG) {
      Timber.w("UI CREATED");
    }

    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

  }

  @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
  public void onEventMainThread(Events.SosMessageSent event) {
    if (messageView != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd h:mm a");
      messageView.setText(sdf.format(Calendar.getInstance().getTime()) + " - Sent");
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.delete: {
        clearPin();
        break;
      }
      case R.id.pad0: {
        addDigitToPin("0");
        break;
      }
      case R.id.pad1: {
        addDigitToPin("1");
        break;
      }
      case R.id.pad2: {
        addDigitToPin("2");
        break;
      }
      case R.id.pad3: {
        addDigitToPin("3");
        break;
      }
      case R.id.pad4: {
        addDigitToPin("4");
        break;
      }
      case R.id.pad5: {
        addDigitToPin("5");
        break;
      }
      case R.id.pad6: {
        addDigitToPin("6");
        break;
      }
      case R.id.pad7: {
        addDigitToPin("7");
        break;
      }
      case R.id.pad8: {
        addDigitToPin("8");
        break;
      }
      case R.id.pad9: {
        addDigitToPin("9");
        break;
      }
    }
    //v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
  }


  // Add digit to PIN and update "pin rings"
  private boolean addDigitToPin(String digit) {
    if (pin.length() < 4) {
      pin += digit;

      switch (pin.length()) {
        case 1:
          pinRing1View.setBackgroundResource(R.drawable.pin_ring_full);
          break;
        case 2:
          pinRing2View.setBackgroundResource(R.drawable.pin_ring_full);
          break;
        case 3:
          pinRing3View.setBackgroundResource(R.drawable.pin_ring_full);
          break;
        case 4: // All done - validate PIN
          pinRing4View.setBackgroundResource(R.drawable.pin_ring_full);
          // Exit boss mode only if allowed
          boolean exitBossModeAllowed = settings.getAgentRoles().exitbossmode();

          if (exitBossModeAllowed && Integer.parseInt(pin) == settings.getBossModePin()) {
            // Exit Boss Mode
            EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_DISABLE));
          }//truv test
          else if (/*!isJdar &&*/ Integer.parseInt(pin) == settings.getPanicDuressPin()) {
            // Send covert SOS
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd h:mm a");
            messageView.setText(sdf.format(Calendar.getInstance().getTime()) + " - Sending...");

            alertManager.startPanicDuress(false);

            EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_DISABLE));
            new Handler().postDelayed(new Runnable() {
              @Override
              public void run() {
                mediaManager.startAudioRecording(15000);
              }
            },500);

          } else {
            Toast.makeText(getActivity(), getString(R.string.invalid_pin_try_again), Toast.LENGTH_SHORT).show();
            clearPin();
          }
          break;
      }

      return true;
    }

    return false;
  }

  // Clear PIN
  private void clearPin() {
    pin = "";
    messageView.setText("");
    pinRing1View.setBackgroundResource(R.drawable.pin_ring_empty);
    pinRing2View.setBackgroundResource(R.drawable.pin_ring_empty);
    pinRing3View.setBackgroundResource(R.drawable.pin_ring_empty);
    pinRing4View.setBackgroundResource(R.drawable.pin_ring_empty);
  }


}
