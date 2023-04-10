package com.example.android13ckckeyboard;

import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;


import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hijamoya.keyboardview.Keyboard;
import com.hijamoya.keyboardview.KeyboardView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener, RecyclerViewAdapter.RecyclerViewClickListener {

    static final int SWITCH_CKC_KEYCODE = -11;
    static final int SWITCH_QWERTY_KEYCODE = -12;
    static final int SWITCH_SYMBOL1_KEYCODE = -13;
    static final int SWITCH_SYMBOL2_KEYCODE = -14;

    static final int CKC_CODE_MAX_LENGTH = 6;
    static final int MAX_SINGLE_IN_SINGLE_PREVIEW = 10;
    static final int MAX_SINGLE_COUNT_IN_WORDS_PREVIEW = 15;
    static final List<String> ckcInputOptions = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0");

    private LinearLayout rootLayout;
    private LinearLayout ckcPreviewBlock;
    private KeyboardView keyboardView;
    private Keyboard ckcKeyboard;
    private Keyboard qwertyKeyboard;
    private Keyboard symbol1Keyboard;
    private Keyboard symbol2Keyboard;
    private Keyboard currentKeyboard;
    private TextView previewInput;
    private RecyclerView previewSingle;
    private RecyclerView previewWords;

    private String ckcInputString = "";
    private Map<String, Map<String, List<String>>> wordMap = new HashMap<>();

    private boolean caps = false;

    @Override
    public View onCreateInputView() {

        // create view
        rootLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.root_layout, null);
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, rootLayout, false);
        ckcKeyboard = new Keyboard(this, R.xml.layout_ckc);
        qwertyKeyboard = new Keyboard(this, R.xml.layout_qwerty);
        symbol1Keyboard = new Keyboard(this, R.xml.layout_symbol_1);
        symbol2Keyboard = new Keyboard(this, R.xml.layout_symbol_2);

        ckcPreviewBlock = rootLayout.findViewById(R.id.ckc_preview);
        previewInput = rootLayout.findViewById(R.id.input_preview);
        previewSingle = rootLayout.findViewById(R.id.single_preview);
        previewWords = rootLayout.findViewById(R.id.words_preview);
        ckcPreviewBlock.setVisibility(View.GONE);

        LinearLayoutManager layoutManager_single = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        previewSingle.setLayoutManager(layoutManager_single);
        LinearLayoutManager layoutManager_word = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        previewWords.setLayoutManager(layoutManager_word);

        // get CKC Data
        InputStream inputStream = null;
        try {
            inputStream = getAssets().open("single.csv");
        } catch (IOException e) {
            Log.d("Keyboard", "error 1 " + e);
            throw new RuntimeException(e);

        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        // Read each line of the CSV file and parse into an array of words
        String line;
        while (true) {
            try {
                if (!((line = reader.readLine()) != null)) break;
            } catch (IOException e) {
                Log.d("Keyboard", "error 2 " + e);
                throw new RuntimeException(e);
            }
            String[] fields = line.split(",");
            String code = fields[0];
            String word = fields[1];
            if (!wordMap.containsKey(code)) {
                wordMap.put(code, new HashMap<>());
                wordMap.get(code).put("single", new ArrayList<>());
                wordMap.get(code).put("words", new ArrayList<>());
            }
            wordMap.get(code).get("single").add(word);
        }

        try {
            inputStream = getAssets().open("words.csv");
        } catch (IOException e) {
            Log.d("Keyboard", "error 1 " + e);
            throw new RuntimeException(e);

        }
        reader = new BufferedReader(new InputStreamReader(inputStream));
        // Read each line of the CSV file and parse into an array of words
        while (true) {
            try {
                if (!((line = reader.readLine()) != null)) break;
            } catch (IOException e) {
                Log.d("Keyboard", "error 2 " + e);
                throw new RuntimeException(e);
            }
            String[] fields = line.split(",");
            String code = fields[0];
            String word = fields[1];
            if (!wordMap.containsKey(code)) {
                wordMap.put(code, new HashMap<>());
                wordMap.get(code).put("single", new ArrayList<>());
                wordMap.get(code).put("words", new ArrayList<>());
            }
            wordMap.get(code).get("words").add(word);
        }
        Log.d("Keyboard", "wordMap " + wordMap.get("012000"));

        // assign views and return
        currentKeyboard = ckcKeyboard;
        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        rootLayout.addView(keyboardView);
        return rootLayout;
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }



    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {

            // general Behavior
            ckcPreviewBlock.setVisibility(View.GONE);
            switch(primaryCode) {
                case SWITCH_CKC_KEYCODE:
                case SWITCH_QWERTY_KEYCODE:
                case SWITCH_SYMBOL1_KEYCODE:
                case SWITCH_SYMBOL2_KEYCODE:
                    switchKeyboardByCode(primaryCode);
                    break;
                case Keyboard.KEYCODE_DELETE :
                    Boolean canDeleteText = true;
                    if (currentKeyboard == ckcKeyboard) {
                        canDeleteText = deleteCkcInput();
                    }
                    if (canDeleteText) {
                        CharSequence selectedText = inputConnection.getSelectedText(0);
                        Log.d("Backspace", String.valueOf(selectedText));
                        if (TextUtils.isEmpty(selectedText)) {
                            inputConnection.deleteSurroundingText(1, 0);
                        } else {
                            inputConnection.commitText("", 1);
                        }
                    }
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboardView.setShifted(caps);
                    keyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    break;
                default :

                    // Keyboard Special Behaviors
                    Boolean isHandleAsCkcInput = false;
                    if (currentKeyboard == ckcKeyboard) {
                        String code = String.valueOf((char) primaryCode);
                        if (ckcInputOptions.contains(code)) {
                            isHandleAsCkcInput = true;
                            ckcKeyActions(code);
                        }
                    }

                    if (!isHandleAsCkcInput) {
                        char code = (char) primaryCode;
                        if(Character.isLetter(code) && caps){
                            code = Character.toUpperCase(code);
                        }
                        inputConnection.commitText(String.valueOf(code), 1);
                    }
            }
            if (ckcInputString != "") {
                ckcPreviewBlock.setVisibility(View.VISIBLE);
            }

        }

    }


    private void switchKeyboardByCode(int keyCode) {
        switch (keyCode) {
            case SWITCH_QWERTY_KEYCODE:
                currentKeyboard = qwertyKeyboard;
                break;
            case SWITCH_CKC_KEYCODE:
                currentKeyboard = ckcKeyboard;
                break;
            case SWITCH_SYMBOL1_KEYCODE:
                currentKeyboard = symbol1Keyboard;
                break;
            case SWITCH_SYMBOL2_KEYCODE:
                currentKeyboard = symbol2Keyboard;
                break;
        }
        keyboardView.setKeyboard(currentKeyboard);
        clearPreview();
    }


    private void ckcKeyActions(String code) {
        String try_ckcInputString = ckcInputString + code;
        Boolean isSuccess = getWordsByCode(try_ckcInputString);
        if (isSuccess) {
            ckcInputString += code;
        }
        previewInput.setText(ckcInputString);
        ckcPreviewBlock.setVisibility(View.VISIBLE);
    }

    private Boolean getWordsByCode(String code) {

        if (code.length() > CKC_CODE_MAX_LENGTH || code.length() == 0) {
            return false;
        }

        Map<String, List<String>> codeInfo = wordMap.get(code);

        if (codeInfo == null || codeInfo.get("words").size() == 0) {
            // try to find the code as prefix for words only
            Set<String> sortedKeys = new TreeSet<>(wordMap.keySet());
            for (String key : sortedKeys) {
                if (key.startsWith(code)) {
                    if (codeInfo == null) {
                        codeInfo = new HashMap<>();
                        codeInfo.put("single", new ArrayList<>());
                        codeInfo.put("words", new ArrayList<>());
                    }
                    if (codeInfo.get("single").size() == 0) {
                        codeInfo.get("single").addAll(wordMap.get(key).get("single"));
                    }
                    if (codeInfo.get("words").size() == 0) {
                        codeInfo.get("words").addAll(wordMap.get(key).get("words"));
                    }
                    if (codeInfo.get("single").size() > 0 && codeInfo.get("words").size() > 0) {
                        break;
                    }
                }
            }
            if (codeInfo == null) {
                return false;
            }
        }

        List previewList_single = codeInfo.get("single");
        Log.d("Keyboard", "gotSingle " + previewList_single);
        RecyclerViewAdapter adapter_single = new RecyclerViewAdapter(previewList_single, this);
        previewSingle.setAdapter(adapter_single);

        List previewList_words = codeInfo.get("words");
        Log.d("Keyboard", "gotWords " + previewList_words);
        RecyclerViewAdapter adapter_word = new RecyclerViewAdapter(previewList_words, this);
        previewWords.setAdapter(adapter_word);
        return true;
    }

    private void selectCkcOption(String word) {
        InputConnection inputConnection = getCurrentInputConnection();
        inputConnection.commitText(word, 1);
        clearPreview();
    }


    private void clearPreview() {
        ckcInputString = "";
        previewInput.setText("");
        ckcPreviewBlock.setVisibility(View.GONE);
    }

    private Boolean deleteCkcInput() {
        Boolean isCkcInputEmpty = true;
        if (ckcInputString != "") {
            isCkcInputEmpty = false;
            ckcInputString = ckcInputString.substring(0, ckcInputString.length() - 1);
            previewInput.setText(ckcInputString);
            Boolean isSuccess = getWordsByCode(ckcInputString);
            if (!isSuccess) {
                clearPreview();
            }
        }
        return isCkcInputEmpty;
    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    @Override
    public void onRecyclerViewItemClick(String word) {
        selectCkcOption(word);
    }
}