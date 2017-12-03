package com.zzzmode.appopsx.ui.main;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.widget.Toast;
import com.zzzmode.appopsx.BuildConfig;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.BaseActivity;
import com.zzzmode.appopsx.ui.analytics.AEvent;
import com.zzzmode.appopsx.ui.analytics.ATracker;
import com.zzzmode.appopsx.ui.core.AppOpsx;
import com.zzzmode.appopsx.ui.core.Helper;
import com.zzzmode.appopsx.ui.core.LangHelper;
import com.zzzmode.appopsx.ui.model.OpEntryInfo;
import com.zzzmode.appopsx.ui.widget.NumberPickerPreference;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.single.SingleJust;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;

/**
 * Created by zl on 2017/1/16.
 */

public class SettingsActivity extends BaseActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setTitle(R.string.menu_setting);
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, new MyPreferenceFragment()).commit();

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    AppOpsx.updateConfig(getApplicationContext());
  }


  public static class MyPreferenceFragment extends PreferenceFragmentCompat implements
      Preference.OnPreferenceClickListener {

    private Preference mPrefAppSort;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.settings, rootKey);

      findPreference("ignore_premission").setOnPreferenceClickListener(this);
      findPreference("show_sysapp").setOnPreferenceClickListener(this);
      findPreference("ifw_enabled")
              .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                  Context context = getActivity().getApplicationContext();
                  Helper.setService(context, "", null)
                          .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                          .subscribe(new ResourceObserver<Boolean>() {
                            @Override
                            public void onNext(Boolean value) {
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onComplete() {
                            }
                          });
                  return true;
                }
              });

      findPreference("project").setOnPreferenceClickListener(this);

      findPreference("opensource_licenses").setOnPreferenceClickListener(this);
      findPreference("help").setOnPreferenceClickListener(this);
      findPreference("translate").setOnPreferenceClickListener(this);

      Preference version = findPreference("version");
      version.setSummary(BuildConfig.VERSION_NAME);
      version.setOnPreferenceClickListener(this);

      Preference appLanguage = findPreference("pref_app_language");
      appLanguage.setOnPreferenceClickListener(this);
      appLanguage.setSummary(
          getResources().getStringArray(R.array.languages)[LangHelper.getLocalIndex(getContext())]);

      findPreference("acknowledgments")
          .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              ATracker.send(AEvent.C_SETTING_KNOWLEDGMENTS);
              StringBuilder sb = new StringBuilder();
              String[] stringArray = getResources().getStringArray(R.array.acknowledgments_list);
              for (String s : stringArray) {
                sb.append(s).append('\n');
              }
              sb.deleteCharAt(sb.length() - 1);
              showTextDialog(R.string.acknowledgments_list, sb.toString());
              return true;
            }
          });

      findPreference("ignore_premission_templete")
          .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              ATracker.send(AEvent.C_SETTING_IGNORE_TEMPLETE);
              showPremissionTemplete();
              return true;
            }
          });

      mPrefAppSort = findPreference("pref_app_sort_type");
      mPrefAppSort.setSummary(getString(R.string.app_sort_type_summary,
          getResources().getStringArray(R.array.app_sort_type)[PreferenceManager
              .getDefaultSharedPreferences(getActivity()).getInt(mPrefAppSort.getKey(), 0)]));
      mPrefAppSort.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          ATracker.send(AEvent.C_SETTING_APP_SORE);
          showAppSortDialog(preference);
          return true;
        }
      });

      findPreference("pref_app_daynight_mode")
          .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

              ATracker.send(AEvent.C_SETTING_SWITCH_THEME);
              return true;
            }
          });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
      if (preference instanceof NumberPickerPreference) {
        DialogFragment fragment = NumberPickerPreference.
            NumberPickerPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager(),
            "NumberPickerPreferenceDialogFragment");
      } else {
        super.onDisplayPreferenceDialog(preference);
      }
    }

    private void closeServer() {
      Helper.closeBgServer(getActivity().getApplicationContext()).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<Boolean>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(Boolean value) {
          Activity activity = getActivity();
          if (activity != null) {
            Toast.makeText(activity, "已关闭", Toast.LENGTH_SHORT).show();
          }
        }

        @Override
        public void onError(Throwable e) {

        }
      });
    }

    private void showPremissionTemplete() {

      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.auto_ignore_permission_title);
      List<OpEntryInfo> localOpEntryInfos = Helper.getLocalOpEntryInfos(getActivity());
      int size = localOpEntryInfos.size();
      CharSequence[] items = new CharSequence[size];

      boolean[] selected = new boolean[size];

      for (int i = 0; i < size; i++) {
        OpEntryInfo opEntryInfo = localOpEntryInfos.get(i);
        items[i] = opEntryInfo.opPermsLab;
        selected[i] = false; //默认关闭
      }

      initCheckd(selected);

      final SparseBooleanArray choiceResult = new SparseBooleanArray();
      for (int i = 0; i < selected.length; i++) {
        choiceResult.put(i, selected[i]);
      }

      saveChoice(choiceResult);

      builder
          .setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
              choiceResult.put(which, isChecked);
            }
          });
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          saveChoice(choiceResult);
        }
      });
      builder.show();
    }

    private void initCheckd(boolean[] localChecked) {
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
      String result = sp
          .getString("auto_perm_templete", getActivity().getString(R.string.default_ignored));
      String[] split = result.split(",");
      for (String s : split) {
        try {
          int i = Integer.parseInt(s);
          localChecked[i] = true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private void saveChoice(SparseBooleanArray choiceResult) {
      StringBuilder sb = new StringBuilder();
      int size = choiceResult.size();
      for (int i = 0; i < size; i++) {
        if (choiceResult.get(i)) {
          sb.append(i).append(',');
        }
      }
      String s = sb.toString();
      if (!TextUtils.isEmpty(s)) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.edit().putString("auto_perm_templete", s).apply();
      }
    }

    private void showTextDialog(int title, String text) {
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(title);
      builder.setMessage(text);
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
    }

    private void showAppSortDialog(final Preference preference) {
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.app_sort_type_title);

      final int[] selected = new int[1];
      selected[0] = PreferenceManager.getDefaultSharedPreferences(getActivity())
          .getInt(preference.getKey(), 0);
      builder.setSingleChoiceItems(R.array.app_sort_type, selected[0],
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              selected[0] = which;
            }
          });

      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
              .putInt(preference.getKey(), selected[0]).apply();
          mPrefAppSort.setSummary(getString(R.string.app_sort_type_summary,
              getResources().getStringArray(R.array.app_sort_type)[selected[0]]));
        }
      });
      builder.show();
    }

    private void showLanguageDialog(final Preference preference) {
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.app_language);

      final int[] selected = new int[1];
      int defSelected = LangHelper.getLocalIndex(getContext());

      builder.setSingleChoiceItems(R.array.languages, defSelected,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              selected[0] = which;
            }
          });

      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          String language = getResources().getStringArray(R.array.languages_key)[selected[0]];
          PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
              .putString(preference.getKey(), language).apply();
          preference.setSummary(getResources().getStringArray(R.array.languages)[selected[0]]);
          ATracker.send(AEvent.C_LANG, Collections.singletonMap("lang", language));
          switchLanguage();
        }
      });
      builder.show();
    }


    private void switchLanguage() {
      LangHelper.updateLanguage(getContext());
      LangHelper.updateLanguage(getContext().getApplicationContext());

      Intent it = new Intent(getActivity(), MainActivity.class);
      it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getActivity().startActivity(it);
      getActivity().finish();
    }

    private void showVersion() {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse("market://details?id=com.zzzmode.appopsx"));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (!getContext().getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
        startActivity(intent);
      } else {
        intent.setData(Uri.parse("https://github.com/8enet/AppOpsX"));
        startActivity(intent);
      }
    }

    private void showShellStart(){
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setMessage(getString(R.string.shell_cmd_help,getString(R.string.shell_cmd)));
      builder.setPositiveButton(android.R.string.copy, new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
          clipboardManager.setPrimaryClip(ClipData.newPlainText(null, getString(R.string.shell_cmd)));
          dialog.dismiss();
          Toast.makeText(getContext(),R.string.copied_hint,Toast.LENGTH_SHORT).show();
        }
      });
      builder.create().show();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
      String key = preference.getKey();
      String id = null;
      if ("ignore_premission".equals(key)) {
        id = AEvent.C_SETTING_AUTO_IGNORE;
      } else if ("show_sysapp".equals(key)) {
        id = AEvent.C_SETTING_SHOW_SYS;
      } else if ("use_adb".equals(key)) {
        id = AEvent.C_SETTING_USE_ADB;
      } else if ("allow_bg_remote".equals(key)) {
        id = AEvent.C_SETTING_ALLOW_BG;
      } else if ("version".equals(key)) {
        showVersion();
        id = AEvent.C_SETTING_VERSION;
      } else if ("project".equals(key)) {
        id = AEvent.C_SETTING_GITHUB;
      } else if ("opensource_licenses".equals(key)) {
        id = AEvent.C_SETTING_OPENSOURCE;
        Intent intent = new Intent(getContext(), HtmlActionActivity.class);
        intent.putExtra(Intent.EXTRA_TITLE, preference.getTitle());
        intent.putExtra(HtmlActionActivity.EXTRA_URL, "file:///android_res/raw/licenses.html");
        getActivity().startActivity(intent);
      } else if ("help".equals(key)) {
        id = AEvent.C_SETTING_HELP;
        Intent intent = new Intent(getContext(), HtmlActionActivity.class);
        intent.putExtra(Intent.EXTRA_TITLE, preference.getTitle());
        intent.putExtra(HtmlActionActivity.EXTRA_URL, "file:///android_res/raw/help.html");
        getActivity().startActivity(intent);
      } else if ("translate".equals(key)) {
        id = AEvent.C_SETTING_TRANSLATE;
      } else if ("pref_app_language".equals(key)) {
        id = AEvent.C_SETTING_LANGUAGE;
        showLanguageDialog(preference);
      } else if ("shell_start".equals(key)){
        id = AEvent.C_SETTING_SHELL_START;
        showShellStart();
      }
      if (id != null) {
        ATracker.send(id);
      }
      return false;
    }
  }
}
