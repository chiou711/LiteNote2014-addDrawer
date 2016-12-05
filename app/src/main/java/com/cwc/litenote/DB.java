package com.cwc.litenote;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DB   
{

    private static Context mContext = null;
    private static DatabaseHelper mDbHelper ;
    private static SQLiteDatabase mDb;
    private static final String DB_NAME = "notes.db";
    private static int DB_VERSION = 1;
    
    private static String mNotesTableId;
    private static String mPlaylistId;

    private static String DB_NOTES_TABLE_PREFIX = "Notes";
    private static String DB_NOTES_TABLE_NAME; // Note: name = prefix + id
    private static String DB_TABS_TABLE_PREFIX = "Tabs";
    private static String DB_PLAYLIST_TABLE_PREFIX = "Playlist";
    private static String DB_PLAYLIST_TABLE_NAME; // Note: name = prefix + id   
    private static String DB_DRAWER_TABLE_NAME = "Drawer";
    
    public static final String KEY_NOTE_ID = "_id"; //do not rename _id for using CursorAdapter 
    public static final String KEY_NOTE_TITLE = "note_title";
    public static final String KEY_NOTE_BODY = "note_body";
    public static final String KEY_NOTE_MARKING = "note_marking";
    public static final String KEY_NOTE_PICTURE_URI = "note_picture_uri";
    public static final String KEY_NOTE_AUDIO_URI = "note__audio_uri";
    public static final String KEY_NOTE_DRAWING_URI = "note__drawing_uri";
    public static final String KEY_NOTE_CREATED = "note_created";
    
    public static final String KEY_TAB_ID = "tab_id"; //can rename _id for using BaseAdapter
    public static final String KEY_TAB_TITLE = "tab_title";
    public static final String KEY_TAB_NOTES_TABLE_ID = "tab_notes_table_id";
    public static final String KEY_TAB_PLAYLIST_TITLE = "tab_playlist_title";
    public static final String KEY_TAB_PLAYLIST_TABLE_ID = "tab_playlist_table_id";
    public static final String KEY_TAB_STYLE = "tab_style";
    public static final String KEY_TAB_CREATED = "tab_created";
    
    public static final String KEY_MEDIA_ID = "media_id";
    public static final String KEY_MEDIA_TITLE = "media_title";
    public static final String KEY_MEDIA_URI_STRING = "media_uri";
    public static final String KEY_MEDIA_MARKING = "media_marking";
    public static final String KEY_MEDIA_CREATED = "media_created";

    public static final String KEY_DRAWER_ID = "drawer_id"; //can rename _id for using BaseAdapter
    public static final String KEY_DRAWER_TABS_TABLE_ID = "drawer_tabs_table_id"; //can rename _id for using BaseAdapter
    public static final String KEY_DRAWER_TITLE = "drawer_title";
    public static final String KEY_DRAWER_CREATED = "drawer_created";
    
	private static int DEFAULT_TAB_COUNT = 5;//10; //first time
	public static int DEFAULT_DRAWER_COUNT = 3;//10; //first time
	

    /** Constructor */
    public DB(Context context) {
        DB.mContext = context;
    }

    public DB open() throws SQLException 
    {
        mDbHelper = new DatabaseHelper(mContext); 
        
        // will call DatabaseHelper.onCreate()first time when database is not created yet
        mDb = mDbHelper.getWritableDatabase();
        return this;  
    }

    public void close() {
        mDbHelper.close(); 
    }
    
    
    private static class DatabaseHelper extends SQLiteOpenHelper
    {  
        public DatabaseHelper(Context context) 
        {  
            super(context, DB_NAME , null, DB_VERSION);
        }

        @Override
        //Called when the database is created ONLY for the first time.
        public void onCreate(SQLiteDatabase db)
        {   
        	String tableCreated;
        	String DB_CREATE;
        	
        	// tables for notes
        	for(int i=1; i<= DEFAULT_DRAWER_COUNT; i++)
        	{
	        	for(int j=1; j<=DEFAULT_TAB_COUNT; j++)
	        	{
	        		tableCreated = DB_NOTES_TABLE_PREFIX.concat(String.valueOf(i)+ "_"+ String.valueOf(j));
		            DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" + 
					            		KEY_NOTE_ID + " INTEGER PRIMARY KEY," +
					            		KEY_NOTE_TITLE + " TEXT," +
					            		KEY_NOTE_PICTURE_URI + " TEXT," +
					            		KEY_NOTE_AUDIO_URI + " TEXT," +
					            		KEY_NOTE_DRAWING_URI + " TEXT," +
					    				KEY_NOTE_BODY + " TEXT," + 
					    				KEY_NOTE_MARKING + " INTEGER," +
					    				KEY_NOTE_CREATED + " INTEGER);";
		            db.execSQL(DB_CREATE);         
	        	}
        	}
        	
        	// table for Tab info
        	for(int i=1;i<= DEFAULT_DRAWER_COUNT; i++)
        	{
        		tableCreated = DB_TABS_TABLE_PREFIX.concat(String.valueOf(i));
	            DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" + 
				            		KEY_TAB_ID + " INTEGER PRIMARY KEY," +
				            		KEY_TAB_TITLE + " TEXT," +
				            		KEY_TAB_NOTES_TABLE_ID + " INTEGER," +
				            		KEY_TAB_PLAYLIST_TITLE + " TEXT," +
				            		KEY_TAB_PLAYLIST_TABLE_ID + " INTEGER," +
				            		KEY_TAB_STYLE + " INTEGER," +
				            		KEY_TAB_CREATED + " INTEGER);";
	            db.execSQL(DB_CREATE);  
        	}
        	
        	// table for Drawer
        	tableCreated = DB_DRAWER_TABLE_NAME;
            DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" + 
			            		KEY_DRAWER_ID + " INTEGER PRIMARY KEY," +
			            		KEY_DRAWER_TABS_TABLE_ID + " INTEGER," +
			            		KEY_DRAWER_TITLE + " TEXT," +
			            		KEY_DRAWER_CREATED + " INTEGER);";
            db.execSQL(DB_CREATE);  
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        { //??? how to upgrade?
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onUpgrade / drop DB / DATABASE_NAME = " + DB_NAME);
     	    onCreate(db);
        }
        
        @Override
        public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion)
        { 
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onDowngrade / drop DB / DATABASE_NAME = " + DB_NAME);
     	    onCreate(db);
        }
    }
    
    /*
     * DB functions
     * 
     */
	public static Cursor mTabCursor;
	public Cursor mNoteCursor;
	public Cursor mMediaCursor;
	public Cursor mDrawerCursor;

	void doOpenByDrawer(int i) {
		this.open();
		mDrawerCursor = this.getDrawerCursor();
		mTabCursor = this.getTabCursorByDrawer(i);
		mNoteCursor = this.getNoteCursor();
	}
	
	void doGetMediaCursor() {
		mMediaCursor = this.getMediaCursor();
	}	
	
	void doClose()	{
		this.close();
	}
	
    // delete DB
	public static void deleteDB()
	{
        mDb = mDbHelper.getWritableDatabase();
        try {
	    	mDb.beginTransaction();
	        mContext.deleteDatabase(DB_NAME);
	        mDb.setTransactionSuccessful();
	    }
	    catch (Exception e) {
	    }
	    finally {
	    	Toast.makeText(mContext,R.string.config_delete_DB_toast,Toast.LENGTH_SHORT).show();
	    	mDb.endTransaction();
	    }
	}         
	
	public static boolean isTableExisted(String tableName) 
	{
	    Cursor cursor = mDb.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+tableName+"'", null);
	    if(cursor!=null) 
	    {
	        if(cursor.getCount()>0) 
	        {
	        	cursor.close();
	            return true;
	        }
	        cursor.close();
	    }
	    return false;
	}		
	
    /**
     *  Note table
     * 
     */
    // table columns: for note
    String[] strNoteColumns = new String[] {
          KEY_NOTE_ID,
          KEY_NOTE_TITLE,
          KEY_NOTE_PICTURE_URI,
          KEY_NOTE_AUDIO_URI,
          KEY_NOTE_DRAWING_URI,
          KEY_NOTE_BODY,
          KEY_NOTE_MARKING,
          KEY_NOTE_CREATED
      };

    //insert new note table
    public void insertNoteTable(int id)
    {   
    	{
    		//format "notesTable1"
        	DB_NOTES_TABLE_NAME = DB_NOTES_TABLE_PREFIX.concat(String.valueOf(getDrawer_TabsTableId())+"_"+String.valueOf(id));
            String dB_insert_table = "CREATE TABLE IF NOT EXISTS " + DB_NOTES_TABLE_NAME + "(" +
            							KEY_NOTE_ID + " INTEGER PRIMARY KEY," +
            							KEY_NOTE_TITLE + " TEXT," +  
            							KEY_NOTE_PICTURE_URI + " TEXT," +  
            							KEY_NOTE_AUDIO_URI + " TEXT," +  
            							KEY_NOTE_DRAWING_URI + " TEXT," +  
            							KEY_NOTE_BODY + " TEXT," +
            							KEY_NOTE_MARKING + " INTEGER," +
            							KEY_NOTE_CREATED + " INTEGER);";
            mDb.execSQL(dB_insert_table);         
    	}
    }

    //delete table
    public void dropNoteTable(int id)
    {   
    	{
    		//format "notesTable1"
        	DB_NOTES_TABLE_NAME = DB_NOTES_TABLE_PREFIX.concat(String.valueOf(id));
            String dB_drop_table = "DROP TABLE IF EXISTS " + DB_NOTES_TABLE_NAME + ";";
            mDb.execSQL(dB_drop_table);         
    	}
    }    
    
    // select all notes
    public Cursor getNoteCursor() {
        return mDb.query(DB_NOTES_TABLE_NAME, 
             strNoteColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }   
    
    //set note table id
    public static void setNotesTableId(String id)
    {
    	mNotesTableId = id;
    	
    	// table number initialization: name = prefix + id
        DB_NOTES_TABLE_NAME = DB_NOTES_TABLE_PREFIX.concat(String.valueOf(getDrawer_TabsTableId())+"_"+mNotesTableId);
    	System.out.println("DB / _setNoteTableId mNoteTableId=" + mNotesTableId);
    }  
    
    //get note table id
    public static String getNotesTableId()
    {
    	return mNotesTableId;
    }  
    
    // Insert note
    // createTime: 0 will update time
    public long insertNote(String title,String pictureUri, String audioUri, String drawingUri, String body, Long createTime)
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_NOTE_TITLE, title);   
        args.put(KEY_NOTE_PICTURE_URI, pictureUri);
        args.put(KEY_NOTE_AUDIO_URI, audioUri);
        args.put(KEY_NOTE_DRAWING_URI, drawingUri);
        args.put(KEY_NOTE_BODY, body);
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, now.getTime());
        else
        	args.put(KEY_NOTE_CREATED, createTime);
        	
        args.put(KEY_NOTE_MARKING,0);
        return mDb.insert(DB_NOTES_TABLE_NAME, null, args);  
    }  
    
    public boolean deleteNote(long rowId) {  
        return mDb.delete(DB_NOTES_TABLE_NAME, KEY_NOTE_ID + "=" + rowId, null) > 0;
    }
    
    //query note
    public Cursor queryNote(long rowId) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
					                DB_NOTES_TABLE_NAME,
					                new String[] {KEY_NOTE_ID,
				  								  KEY_NOTE_TITLE,
				  								  KEY_NOTE_PICTURE_URI,
				  								  KEY_NOTE_AUDIO_URI,
				  								  KEY_NOTE_DRAWING_URI,
        										  KEY_NOTE_BODY,
        										  KEY_NOTE_MARKING,
        										  KEY_NOTE_CREATED},
					                KEY_NOTE_ID + "=" + rowId,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    // update note
    // 		createTime:  0 for Don't update time
    public boolean updateNote(long rowId, String title, String pictureUri, String audioUri, String drawingUri, String body, long marking, long createTime) { 
        ContentValues args = new ContentValues();
        args.put(KEY_NOTE_TITLE, title);
        args.put(KEY_NOTE_PICTURE_URI, pictureUri);
        args.put(KEY_NOTE_AUDIO_URI, audioUri);
        args.put(KEY_NOTE_DRAWING_URI, drawingUri);
        args.put(KEY_NOTE_BODY, body);
        args.put(KEY_NOTE_MARKING, marking);
        
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, mNoteCursor.getLong(mNoteCursor.getColumnIndex(KEY_NOTE_CREATED)));
        else
        	args.put(KEY_NOTE_CREATED, createTime);

        int cUpdateItems = mDb.update(DB_NOTES_TABLE_NAME, args, KEY_NOTE_ID + "=" + rowId, null);
        return cUpdateItems > 0;
    }    

	int getNotesCount()
	{
		return mNoteCursor.getCount();
	}

	public int getMaxNoteId() {
		Cursor cursor = this.getNoteCursor();
		int total = cursor.getColumnCount();
		int iMax =1;
		int iTemp = 1;
		for(int i=0;i< total;i++)
		{
			cursor.moveToPosition(i);
			iTemp = cursor.getInt(cursor.getColumnIndex(KEY_NOTE_ID));
			iMax = (iTemp >= iMax)? iTemp: iMax;
		}
		return iMax;
	}
	
	int getCheckedNotesCount()
	{
		int cCheck =0;
		for(int i=0;i< getNotesCount() ;i++)
		{
			if(getNoteMarking(i) == 1)
				cCheck++;
		}
		return cCheck;
	}		
	
	
	// get note by Id
	public String getNoteTitleById(Long mRowId) 
	{
		return queryNote(mRowId).getString(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_TITLE));
	}
	
	public String getNoteBodyById(Long mRowId) 
	{
		return  queryNote(mRowId).getString(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_BODY));
	}

	public String getNotePictureUriById(Long mRowId)
	{
        Cursor noteCursor = queryNote(mRowId);
		String pictureFileName = noteCursor.getString(noteCursor
														.getColumnIndexOrThrow(DB.KEY_NOTE_PICTURE_URI));
		return pictureFileName;
	}
	
	public String getNoteAudioUriById(Long mRowId)
	{
        Cursor noteCursor = queryNote(mRowId);
		String pictureFileName = noteCursor.getString(noteCursor
														.getColumnIndexOrThrow(DB.KEY_NOTE_AUDIO_URI));
		return pictureFileName;
	}	
	
	public String getNoteDrawingUriById(Long mRowId)
	{
        Cursor noteCursor = queryNote(mRowId);
		String pictureFileName = noteCursor.getString(noteCursor
														.getColumnIndexOrThrow(DB.KEY_NOTE_DRAWING_URI));
		return pictureFileName;
	}		
	
	public Long getNoteMarkingById(Long mRowId) 
	{
		return  queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_MARKING));
	}

	public Long getNoteCreatedTimeById(Long mRowId)
	{
		return  queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_CREATED));
	}

	// get note by position
	Long getNoteId(int position)
	{
		mNoteCursor.moveToPosition(position);
        return (long) mNoteCursor.getInt(mNoteCursor.getColumnIndex(KEY_NOTE_ID));
	}
	
	String getNoteTitle(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_TITLE));
	}
	
	String getNoteBody(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_BODY));
	}

	String getNotePictureUri(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_PICTURE_URI));
	}
	
	String getNoteAudioUri(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_AUDIO_URI));
	}	

	String getNoteDrawingUri(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_DRAWING_URI));
	}	
	
	int getNoteMarking(int position)
	{
		mNoteCursor.moveToPosition(position);
		return mNoteCursor.getInt(mNoteCursor.getColumnIndex(KEY_NOTE_MARKING));
	}
	
	Long getNoteCreatedTime(int position)
	{
		mNoteCursor.moveToPosition(position);
		return mNoteCursor.getLong(mNoteCursor.getColumnIndex(KEY_NOTE_CREATED));
	}

    /*
     * Tab 
     * 
     */
	
    // table columns: for tab info
    String[] strTabColumns = new String[] {
            KEY_TAB_ID,
            KEY_TAB_TITLE,
            KEY_TAB_NOTES_TABLE_ID,
            KEY_TAB_PLAYLIST_TITLE,
            KEY_TAB_PLAYLIST_TABLE_ID,
            KEY_TAB_STYLE,
            KEY_TAB_CREATED
        };   

    // select tabs cursor
    public Cursor getTabCursorByDrawer(int i) {
        return mDb.query(DB_TABS_TABLE_PREFIX + String.valueOf(i), 
             strTabColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }     
    
    // insert tab
    public long insertTab(String intoTable,String title,long ntId, String plTitle, long plId, int style) 
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_TAB_TITLE, title);
        args.put(KEY_TAB_NOTES_TABLE_ID, ntId);
        args.put(KEY_TAB_PLAYLIST_TITLE, plTitle);
        args.put(KEY_TAB_PLAYLIST_TABLE_ID, plId);
        args.put(KEY_TAB_STYLE, style);
        args.put(KEY_TAB_CREATED, now.getTime());
        return mDb.insert(intoTable, null, args);  
    }    
    
    // delete tab
    public long deleteTab(String table,int id) 
    { 
        return mDb.delete(table, KEY_TAB_ID + "='" + id +"'", null);  
    }

    //query single tab info
    public Cursor queryTab(String table, long id) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
        							table,
					                new String[] {KEY_TAB_ID,
        										  KEY_TAB_TITLE,
        										  KEY_TAB_NOTES_TABLE_ID,
        										  KEY_TAB_PLAYLIST_TITLE,
        										  KEY_TAB_PLAYLIST_TABLE_ID,
        										  KEY_TAB_STYLE,
        										  KEY_TAB_CREATED},
					                KEY_TAB_ID + "=" + id,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }

        return mCursor;
    }
    
    //update tab
    public boolean updateTab(long id, String title, long ntId, String plTitle, long plId, int style) 
    { 
        ContentValues args = new ContentValues();
        Date now = new Date(); 
        args.put(KEY_TAB_TITLE, title);
        args.put(KEY_TAB_NOTES_TABLE_ID, ntId);
        args.put(KEY_TAB_PLAYLIST_TITLE, plTitle);
        args.put(KEY_TAB_PLAYLIST_TABLE_ID, plId);
        args.put(KEY_TAB_STYLE, style);
        args.put(KEY_TAB_CREATED, now.getTime());
        return mDb.update(DB_TABS_TABLE_PREFIX+String.valueOf(getDrawer_TabsTableId()), args, KEY_TAB_ID + "=" + id, null) > 0;
    }    
    
	static int getTabsCount()	
	{
		return mTabCursor.getCount();
	}

	int getTabId(int position) 
	{
		mTabCursor.moveToPosition(position);
        return mTabCursor.getInt(mTabCursor.getColumnIndex(KEY_TAB_ID));
	}

    //get current tab info title
    public static String getCurrentTabTitle()
    {
    	String title = null;
    	for(int i=0;i< getTabsCount(); i++ )
    	{
    		if( Integer.valueOf(getNotesTableId()) == getTab_NotesTableId(i))
    		{
    			title = getTabTitle(i);
    		}
    	}
    	return title;
    }     	

	static int getTab_NotesTableId(int position)	
	{
		mTabCursor.moveToPosition(position);
        return mTabCursor.getInt(mTabCursor.getColumnIndex(KEY_TAB_NOTES_TABLE_ID));
	}
	
	static int getTabPlaylistId(int position)	
	{
		mTabCursor.moveToPosition(position);
        return mTabCursor.getInt(mTabCursor.getColumnIndex(KEY_TAB_PLAYLIST_TABLE_ID));
	}
	
	static String getTabPlaylistTitle(int position)	
	{
		mTabCursor.moveToPosition(position);
        return mTabCursor.getString(mTabCursor.getColumnIndex(KEY_TAB_PLAYLIST_TITLE));
	}		
	
	static String getTabTitle(int position) 
	{
		mTabCursor.moveToPosition(position);
        return mTabCursor.getString(mTabCursor.getColumnIndex(KEY_TAB_TITLE));
	}
	
	int getTabStyle(int position)	
	{
		mTabCursor.moveToPosition(position);
        return mTabCursor.getInt(mTabCursor.getColumnIndex(KEY_TAB_STYLE));
	}
    
    /*
     * Media
     * 
     */
    // table columns: for play list
    String[] strMediaColumns = new String[] {
            KEY_MEDIA_ID,
            KEY_MEDIA_TITLE,
            KEY_MEDIA_URI_STRING,
            KEY_MEDIA_MARKING,
            KEY_MEDIA_CREATED
        };    
    
    // select all medium
    public Cursor getMediaCursor() {
        return mDb.query(DB_PLAYLIST_TABLE_NAME, 
             strMediaColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }      
    
    //set playlist id
    public static void setPlaylistId(String id)
    {
    	mPlaylistId = id;
    	
    	// table number initialization: name = prefix + id
        DB_PLAYLIST_TABLE_NAME = DB_PLAYLIST_TABLE_PREFIX.concat(id);
    	System.out.println("DB / _setPlaylistId mPlaylistId=" + mPlaylistId);
    }  
    
    //get note table id
    public static String getPlaylistId()
    {
    	return mPlaylistId;
    }  
    
    //get note table name, name = prefix + id
    public static String getPlaylistTitle()
    {
    	return DB_PLAYLIST_TABLE_NAME;
    }     
    
    //insert new playlist
    public void insertNewPlaylistId(String id)
    {   
    	{
    		
        	// table for Media
    		DB_PLAYLIST_TABLE_NAME = DB_PLAYLIST_TABLE_PREFIX.concat(id);
    		String dB_insert_playlist = "CREATE TABLE IF NOT EXISTS " + 
    							DB_PLAYLIST_TABLE_NAME + "(" + 
			            		KEY_MEDIA_ID + " INTEGER PRIMARY KEY," +
			            		KEY_MEDIA_TITLE + " TEXT," + 
			            		KEY_MEDIA_URI_STRING + " TEXT," + 
			            		KEY_MEDIA_MARKING + " INTEGER," + 
			            		KEY_MEDIA_CREATED + " INTEGER);";
            mDb.execSQL(dB_insert_playlist);  
    	}
    }
    
    // insert media
    public long insertMedia(String title,String uri) 
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_MEDIA_TITLE, title);
        args.put(KEY_MEDIA_URI_STRING, uri);
        args.put(KEY_MEDIA_MARKING, 1); // default: marked
        args.put(KEY_MEDIA_CREATED, now.getTime());
        return mDb.insert(DB_PLAYLIST_TABLE_NAME, null, args);  
    }    
    
    // delete tab info
    public long deleteMedia(long id) 
    { 
        return mDb.delete(DB_PLAYLIST_TABLE_NAME, KEY_MEDIA_ID + "='" + id +"'", null);  
    }

    //query single tab info
    public Cursor queryMedia(String tbl, long id) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
        						   DB_PLAYLIST_TABLE_NAME,
        						   new String[] {KEY_MEDIA_ID,
        										 KEY_MEDIA_TITLE,
        										 KEY_MEDIA_URI_STRING,
        										 KEY_MEDIA_CREATED,
        										 KEY_MEDIA_MARKING},
        										 KEY_MEDIA_ID + "=" + id,
        										 null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    //update new table of tab info
    public boolean updateMedia(long id, String title, String uriStr, int marking) 
    { 
        ContentValues args = new ContentValues();
        Date now = new Date(); 
        args.put(KEY_MEDIA_ID, id);
        args.put(KEY_MEDIA_TITLE, title);
        args.put(KEY_MEDIA_URI_STRING, uriStr);
        args.put(KEY_MEDIA_MARKING, marking);
        args.put(KEY_MEDIA_CREATED, now.getTime());
        return mDb.update(DB_PLAYLIST_TABLE_NAME, args, KEY_MEDIA_ID + "=" + id, null) > 0;
    }    
    
	int getMediumCount()	
	{
		return mMediaCursor.getCount();
	}

	long getMediaId(int position) 
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getLong(mMediaCursor.getColumnIndex(KEY_MEDIA_ID));
	}

	String getMediaTitle(int position) 
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getString(mMediaCursor.getColumnIndex(KEY_MEDIA_TITLE));
	}
	
	String getMediaUri(int position)	
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getString(mMediaCursor.getColumnIndex(KEY_MEDIA_URI_STRING));
	}
	
	int getMediaMarking(int position)	
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getInt(mMediaCursor.getColumnIndex(KEY_MEDIA_MARKING));
	}	
    
	
	/*
	 * Drawer
	 * 
	 * 
	 */
    static int mDrawer_tabsTableId;
    
    // table columns: for drawer
    String[] strDrawerColumns = new String[] {
    	KEY_DRAWER_ID,
    	KEY_DRAWER_TABS_TABLE_ID,
    	KEY_DRAWER_TITLE,
    	KEY_DRAWER_CREATED
      };
    
    
    public Cursor getDrawerCursor() {
        return mDb.query(DB_DRAWER_TABLE_NAME, 
        	 strDrawerColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }   
    
    //query note
    public Cursor queryDrawer(long rowId) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
					                DB_DRAWER_TABLE_NAME,
					                new String[] {KEY_DRAWER_ID,
        										  KEY_DRAWER_TABS_TABLE_ID,
        										  KEY_DRAWER_TITLE,
        										  KEY_DRAWER_CREATED},
        							KEY_DRAWER_ID + "=" + rowId,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public long insertDrawer(int tableId, String title) 
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_DRAWER_TABS_TABLE_ID, tableId);
        args.put(KEY_DRAWER_TITLE, title);
        args.put(KEY_DRAWER_CREATED, now.getTime());
        return mDb.insert(DB_DRAWER_TABLE_NAME, null, args);  
    }       
    
    // update drawer
    public boolean updateDrawer(long rowId, int tabInfoTableId, String drawerTitle) { 
        ContentValues args = new ContentValues();
        Date now = new Date();  
        args.put(KEY_DRAWER_TABS_TABLE_ID, tabInfoTableId);
        args.put(KEY_DRAWER_TITLE, drawerTitle);
       	args.put(KEY_DRAWER_CREATED, now.getTime());

        int cUpdateItems = mDb.update(DB_DRAWER_TABLE_NAME, args, KEY_DRAWER_ID + "=" + rowId, null);
        return cUpdateItems > 0;
    }    
    
    long getDrawerId(int position)
    {
    	mDrawerCursor.moveToPosition(position);
        return (long) mDrawerCursor.getInt(mDrawerCursor.getColumnIndex(KEY_DRAWER_ID));
    }
    
    static void setDrawerTabsTableId(int i)
    {
    	mDrawer_tabsTableId = i;
    }
    
    static int getDrawer_TabsTableId()
    {
    	return mDrawer_tabsTableId;
    }
    
    int getDrawersCount()
    {
    	return mDrawerCursor.getCount();
    }
    
    int getDrawerTabsTableId(int position)
    {
		mDrawerCursor.moveToPosition(position);
        return mDrawerCursor.getInt(mDrawerCursor.getColumnIndex(KEY_DRAWER_TABS_TABLE_ID));
    	
    }
    
	String getDrawerTitle(int position)	
	{
		mDrawerCursor.moveToPosition(position);
        return mDrawerCursor.getString(mDrawerCursor.getColumnIndex(KEY_DRAWER_TITLE));
	}
	
	// get note by Id
	public String getDrawerTitleById(Long mRowId) 
	{
		return queryDrawer(mRowId).getString(queryDrawer(mRowId)
											.getColumnIndexOrThrow(DB.KEY_DRAWER_TITLE));
	}
    
	// get current tabs table
	public static String getCurrentTabsTableName()
	{
		return DB.DB_TABS_TABLE_PREFIX + DB.getDrawer_TabsTableId();
	}
}