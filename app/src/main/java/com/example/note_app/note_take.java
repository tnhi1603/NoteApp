package com.example.note_app;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class note_take extends AppCompatActivity {

    private ImageButton buttonSetting, btnShare;
    EditText edtnotetitle, edtnotecontent;
    ImageButton buttonBack, btnSaveNote;
    boolean isEditMode = false;
    String notetitle, notecontent, noteday, docId;
    TextView noteDay, countCharacter;
    Button btnDelete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_take);
        findbyviewIds();
        CheckBox cbFavorite = findViewById(R.id.cbFavorite);

        // click vao 1 item: 1. get dữ liệu từ intent trước (day_main)
        Intent intent = getIntent();
        if (intent != null) {
            notetitle = intent.getStringExtra("NOTE_TITLE");
            notecontent = intent.getStringExtra("NOTE_CONTENT");
            noteday = intent.getStringExtra("NOTE_DATE");
            edtnotecontent.setText(notecontent);
            edtnotetitle.setText(notetitle);
            noteDay.setText(noteday);
        }

        buttonSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSetting();
            }
        });
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharenote();
            }
        });
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBack();
            }
        });

        // Thêm sự kiện nghe cho CheckBox
        cbFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Xử lý khi trạng thái của CheckBox thay đổi
            if (isChecked) {
                // Note được đánh dấu là yêu thích
                Utility.showToast(note_take.this, "Note đã được đánh dấu là yêu thích");
            } else {
                // Note không được đánh dấu là yêu thích
                Utility.showToast(note_take.this, "Note không còn là yêu thích");
            }
        });

        //Save note
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
                // Khi lưu note thì sẽ thoát ra khỏi note và quay về trang menu
                openBack();
            }
        });

        //Delete note
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNoteFromFirebase();
            }
        });

        // Đọc các cài đặt font và size từ SharedPreferences
        SharedPreferences preferences = getSharedPreferences("MyPreferences", MODE_PRIVATE);

        String fontName = preferences.getString("selectedFont", null);
        Typeface typeface = Typeface.DEFAULT;
        if (fontName != null) {
            try {
                typeface = Typeface.createFromAsset(getAssets(), fontName);
            } catch (Exception e) {
                Log.e("SettingDayActivity", "Failed to create typeface from file", e);
                Toast.makeText(getApplicationContext(), "Failed to create typeface from file", Toast.LENGTH_SHORT).show();
            }
        }

        float textSize = preferences.getFloat("selectedTextSize", 16);

        EditText edt_note_title = findViewById(R.id.edt_note_title);
        TextView note_day = findViewById(R.id.note_day);
        TextView count_character_note = findViewById(R.id.count_character_note);
        EditText edt_note_content = findViewById(R.id.edt_note_content);
        Button btnDeleteNote = findViewById(R.id.btnDeleteNote);

        edt_note_title.setTypeface(typeface);
        //edt_note_title.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        note_day.setTypeface(typeface);
        note_day.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        count_character_note.setTypeface(typeface);
        count_character_note.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        edt_note_content.setTypeface(typeface);
        edt_note_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        btnDeleteNote.setTypeface(typeface);
        btnDeleteNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
}

    //ngoài onCreate
    public void sharenote(){
        notetitle = edtnotetitle.getText().toString();
        notecontent = edtnotecontent.getText().toString();
        String fullNote = "Tiêu đề: " + notetitle + "\n\n" + "Nội dung: " +notecontent ;

        //Tạo intent chia sẻ nội dung
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, fullNote);
        startActivity(Intent.createChooser(shareIntent,"Chia sẻ nội dung ghi chú"));
    }
    public void openSetting()
    {
        Intent intent = new Intent(this, note_take.class);
        startActivity(intent);
        finish();
    }

    public void openBack(){
        Intent intent = new Intent(note_take.this, main.class);
        startActivity(intent);
        finish();
    }

        // Save note
    public void saveNote(){
        String noteTitle = edtnotetitle.getText().toString();
        String noteContent = edtnotecontent.getText().toString();
        if(noteTitle == null || noteTitle.isEmpty()){
            edtnotetitle.setError("Hãy nhập chủ đề");
            return;
        }
        Note note = new Note();
        note.setNote_title(noteTitle);
        note.setNote_content(noteContent);

        // Kiểm tra trạng thái của CheckBox
        CheckBox checkBoxFavorite = findViewById(R.id.cbFavorite);
        boolean isFavorite = checkBoxFavorite.isChecked();
        note.setFavorite(isFavorite);

       // note.setTimestamp(Timestamp.now());
        long time = System.currentTimeMillis();
        String formatTimestamp = formatTimestamp(time);
        // save time vào textview ngày tháng năm
        noteDay.setText("" + formatTimestamp);
        String note_day = formatTimestamp;
        // save time vào note_day
        note.setNote_day(note_day);
        saveNoteToFireBase(note);
        //Chưa update số ký tự được
        countCharacter = (TextView) findViewById(R.id.count_character_note);
        edtnotecontent = (EditText) findViewById(R.id.edt_note_content);
        //int countContent = 0;
    }

    public void saveNoteToFireBase(Note note){
        DocumentReference documentReference;
        if(isEditMode){
            //update the note
            documentReference = Utility.getCollectionReferenceForNotes().document(docId);
        }else{
            //create new note
            documentReference = Utility.getCollectionReferenceForNotes().document();
        }
        documentReference.set(note).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    //note is added
                    Utility.showToast(note_take.this,"Note added successfully");
                    //finish();
                }else{
                    Utility.showToast(note_take.this,"Failed while adding note");
                }
            }
        });
    }

    public String formatTimestamp(long timestamp) {
        // Tạo đối tượng SimpleDateFormat với định dạng mong muốn
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        // Tạo đối tượng Date từ timestamp
        Date date = new Date(timestamp);
        // Định dạng Date thành chuỗi ngày/thời gian
        return sdf.format(date);
    }

    // delete note
    public void deleteNoteFromFirebase(){
        DocumentReference documentReference;
            documentReference = Utility.getCollectionReferenceForNotes().document(docId);
        documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    //note is deleted
                    Utility.showToast(note_take.this,"Note deleted successfully");
                    //finish();
                }else{
                    Utility.showToast(note_take.this,"Failed while deleting note");
                }
            }
        });
    }
    private void findbyviewIds(){
        edtnotetitle = (EditText) findViewById(R.id.edt_note_title);
        edtnotecontent= (EditText) findViewById(R.id.edt_note_content);
        noteDay = (TextView) findViewById(R.id.note_day);
        buttonSetting = (ImageButton) findViewById(R.id.ImageButtonSetting);
        btnShare = (ImageButton) findViewById(R.id.imgbtn_share);
        buttonBack = (ImageButton) findViewById(R.id.ImageButtonBack);
        btnSaveNote = (ImageButton) findViewById(R.id.iBt_Save);
        btnDelete = (Button) findViewById(R.id.btnDeleteNote);
    }
}
