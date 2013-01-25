/*
 *	SOPASPlayerActivity.java
 
 Copyright (c) 2012, AIST
 
 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 of the Software, and to permit persons to whom the Software is furnished to do
 so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 *
 */

package net.sample.pandroid;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SOPAPlayerActivity extends Activity implements OnItemSelectedListener {
	int xPos = 0;									// X-axis position of the image
	int nTask,nMsg,nXshift,nHeight,nYconv;
	int nXwidth = 0;
	int nSampleRate = 44100;
	int iSize = 2048;
	int nParam = 0;
	Bitmap bMap; 
	float screenWidth,screenHeight,dRat;
	float panWidth;
	double dVal;
	double dRot = 0;									// Rotation factor
    short sHrtf[] = new short[36864];				// HRTF database (level)
    short sPhase[] = new short[36864];				// HRTF database (phase)
    boolean isPlay,isErr,isSpinner = false;
    boolean isAdded = false;
    boolean isStarted = false;
    String str;
    String strGif;
	Button button0;
	ArrayAdapter<String> adapter;
	private SQLiteDatabase dbObject;
    private final static String DB_TABLE = "HistoryTable";
	
	private final int MaxColumn = 8;
	private static final int EXTERNAL_BUFFER_SIZE = 8192;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nTask = 0;
        nMsg = 0;
        
        final EditText editText;
        final Spinner spinner;
        int ot = getResources().getConfiguration().orientation;
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        if(ot == Configuration.ORIENTATION_PORTRAIT){
            screenWidth = (float) metrics.heightPixels;
            screenHeight = (float) metrics.widthPixels;
        }
        else{
        	screenWidth = (float) metrics.widthPixels;
        	screenHeight = (float) metrics.heightPixels;
        }
        panWidth = screenWidth * 4;
        
        URL url = null;
        HttpURLConnection httpCon = null;
        InputStream input;
        final bitmapViewTest bMapView = new bitmapViewTest(this);
        
        setContentView(R.layout.main);
        
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.linearLayout);
        
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbObject = dbHelper.getWritableDatabase();
        
        editText = (EditText)findViewById(R.id.editText1);
        spinner = (Spinner) findViewById(R.id.spinner);
        
//		String[] labels = getResources().getStringArray(R.array.value);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		
		int itemNumber = 0;
		
		while(itemNumber >= 0){
			try{
				String readStr = readFromDB(itemNumber);
				adapter.add(readStr);
				itemNumber ++;
			}catch(Exception e){
				if(itemNumber == 0)
					showDialog(this,"Error","Failed to read from database");
				itemNumber = -2;
			}
		}
		
		if(adapter.getCount() == 0)
			adapter.add("http://staff.aist.go.jp/ashihara-k/resource/sopa22k.sopa");
		adapter.setDropDownViewResource(R.layout.spinner_dropdown);
	
		spinner.setAdapter(adapter);
		spinner.setPrompt("Choose an item");
		spinner.setSelection(adapter.getCount() - 1);
		spinner.setOnItemSelectedListener(this);

		button0 = (Button)findViewById(R.id.button0);
        button0.setOnClickListener(new View.OnClickListener(){
        	 @SuppressLint({ "NewApi", "NewApi" })
			public void onClick(View v) {
        	     xPos = 0;
        	     dRot = 0;
        	     if(isSpinner){
        	    	 str = spinner.getSelectedItem().toString();
        	    	 editText.setText(str);
        	    	 isSpinner = false;
        	     }
        	     else
        	    	 str = editText.getText().toString();
        		 String stGif;
        		 if(str.indexOf(".sopa") == -1){
        			 str = str.concat(".sopa");
        			 editText.setText(str);
        		 }
//             	get url of gif
                 int dotIndex = str.lastIndexOf(".");
                 int slashIndex = str.lastIndexOf('/');
                 stGif = str.substring(0,dotIndex);
                 strGif = stGif.concat(".gif");
        	     if(str.indexOf("://") != -1){
                	 String urlString = str.substring(0,dotIndex);
                	 urlString = urlString.concat(".gif");
                     if(!Bmp(urlString) && slashIndex != -1){
                    	 String defString = str.substring(0,slashIndex);
                    	 defString = defString.concat("/default.gif");
                         if(!Bmp(defString)){
                        	 defString = "default.gif";
                             if(!Bmp(defString)){
                            	 nMsg = -1;
                             }
                             else
                            	 nMsg = 0;
                         }
                         else
                        	 nMsg = 0;
                     }
                     else
                    	 nMsg = 0;
        	     }
        	     else{
//        			 editText.setText(strGif);
        	    	 if(!Bmp(strGif)){
                    	 String defString = "default.gif";
                         if(!Bmp(defString)){
                        	 nMsg = -1;
                         }
                         else
                        	 nMsg = 0;
        	    	 }
        	    	 else
        	    		 nMsg = 0;
        	     }
                 // Hide soft keyboard
                 InputMethodManager imm 
                 	= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                 	imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);	
                 bMapView.invalidate();
             }
        });
//		str = editText.getText().toString();
		str = spinner.getSelectedItem().toString();
		if(str.indexOf(".sopa") == -1)
			 str = str.concat(".sopa");
        int dotIndex = str.lastIndexOf(".");
        strGif = str.substring(0,dotIndex);
        strGif = strGif.concat(".gif");
        if(str.indexOf("://") != -1){
        	try{
        		url = new URL(strGif);
        		httpCon = (HttpURLConnection)url.openConnection();
        		httpCon.setDoInput(true);   
        		httpCon.connect();   
        		input = httpCon.getInputStream();   
        		bMap = BitmapFactory.decodeStream(input);   
        		input.close();
        	}catch(IOException e){
        		nMsg = -1;
        	}
        }
        else{
        	try{
        		input = getAssets().open(strGif);
        		bMap = BitmapFactory.decodeStream(input);   
        		input.close();
        		nMsg = 0;
        	}catch(IOException e){
        		nMsg = -1;
        		nXshift = 0;
        	}
        }
        if(nMsg == -1){
        	try{
            	input = getAssets().open("default.gif");
        		bMap = BitmapFactory.decodeStream(input);   
        		input.close();
        		nMsg = 0;
        	}catch(IOException e){
        		nMsg = -1;
        		nXshift = 0;
        	}
        }
    	nXwidth = bMap.getWidth();
    	nHeight = bMap.getHeight();
    	nXshift = nXwidth / 72;
        dRat = panWidth / (float)nXwidth;
        dVal = (double)nHeight * dRat;
        nYconv = (int)dVal;
        
/*        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS); */
        linearLayout.requestFocus();
        linearLayout.addView(bMapView);

    }
    private void showDialog(Context context,
			String title, String text) {
		// TODO Auto-generated method stub
		AlertDialog.Builder varAlertDialog = new AlertDialog.Builder(context);
		varAlertDialog.setTitle(title);
		varAlertDialog.setMessage(text);
		varAlertDialog.setPositiveButton("OK",null);
		varAlertDialog.show();
	}
	private boolean Bmp(String st){
    	int ret = 0;
        if(st.indexOf("://") != -1){
        	try{
        		URL url = new URL(st);
        		HttpURLConnection httpCon = (HttpURLConnection)url.openConnection();
        		httpCon.setDoInput(true);   
        		httpCon.connect();   
        		InputStream input = httpCon.getInputStream();   
        		bMap = BitmapFactory.decodeStream(input);  
        		input.close();
        	}catch(MalformedURLException e){
        		ret = -1;
        	}catch(IOException ex){
        		ret = -1;
        	}catch(Exception e){
        		ret = -1;
        	}
        }
        else{
    		ret = 0;
    		try{
    			InputStream input = getAssets().open(st);
        		bMap = BitmapFactory.decodeStream(input);  
        		input.close();
    		}catch(IOException exp){
    			ret = -2;
    		}
    	}
    	if(ret == -2){
    		ret = 0;
    		try{
    			InputStream input = getAssets().open("default.gif");
        		bMap = BitmapFactory.decodeStream(input);  
        		input.close();
    		}catch(IOException exp){
    			ret = -1;
    		}
    	}
    	nXwidth = bMap.getWidth();
    	nHeight = bMap.getHeight();
    	nXshift = nXwidth / 72;
        dRat = panWidth / (float)nXwidth;
        dVal = (double)nHeight * dRat;
        nYconv = (int)dVal;
        if(ret == 0)
        	return true;
        else
        	return false;
    }
    
    class bitmapViewTest extends View{
    	float touchBegin,touchIniPos;
    	float touchPrev;
//    	TranslateAnimation anim;
    	public bitmapViewTest(Context context){
    		super(context); 
    	}
    	
    	public void renewView(){
    		this.invalidate();
    	}

        public boolean onTouchEvent(MotionEvent e){
    		float x;
    		int nInt,slushIndex;
    		short sSh;
    		double dTmp;
    		String binStr,binPhase;
    		
    		if(e.getY() >= nYconv)
    			return true;	
    		
	        switch(e.getAction()){
	        case MotionEvent.ACTION_DOWN:
	        	touchPrev = touchIniPos = touchBegin = e.getX();
	        	break;
	        case MotionEvent.ACTION_MOVE:
	        	x = e.getX();
	        	float panMoved = x - touchBegin;
	        	touchPrev = touchBegin;
	        	touchBegin = x;
	        	panMoved *= 72;
	        	panMoved /= panWidth;
	        	dRot += panMoved;
	        	if(dRot > 36)
	        		dRot -= 72;
	        	else if(dRot <= -36)
	        		dRot += 72;
	        	dTmp = (double)nXshift * dRot;
	        	xPos = (int)dTmp;
				this.invalidate();
	        	break;
	        	
    		case MotionEvent.ACTION_UP:
    			x = e.getX();
    			if(x - touchIniPos < 2 && x - touchIniPos > -2){
    				if(nMsg >= 5){
 //   					nTask = 0;
    					nMsg = 0;
    				}
    				else if(nMsg == 1 || nMsg == 2)
    					return true;
    				nMsg ++;
    				if(nMsg == 1){					// Prepare HRTF database
    					HttpURLConnection httpCon0 = null;
    					HttpURLConnection httpCon1 = null;
    					InputStream input = null;
    					BufferedInputStream bis = null;
    					BufferedInputStream bin = null;

    					if(str.indexOf("://") != -1){
    						slushIndex = str.lastIndexOf("/");
    						String tmpStr = str.substring(0,slushIndex);
    						binPhase = tmpStr.concat("/phase512.bin");
    						tmpStr = str.substring(0,slushIndex);
    						binStr = tmpStr.concat("/hrtf512.bin");
    						try{
    							URL url0 = new URL(binStr);
    							URL url1 = new URL(binPhase);
    							httpCon0 = (HttpURLConnection)url0.openConnection();
    							if(httpCon0 != null){
    								httpCon0.setRequestMethod("GET");
    								httpCon0.connect();
    								bis = new BufferedInputStream(httpCon0.getInputStream());
    							}
    							else{
    								input = getAssets().open("hrtf512.bin");
    								bis = new BufferedInputStream(input);
    							}
    							DataInputStream dis0 = new DataInputStream(bis);
    							httpCon1 = (HttpURLConnection)url1.openConnection();
    							if(httpCon1 != null){
    								httpCon1.setRequestMethod("GET");
    								httpCon1.connect();
    								bin = new BufferedInputStream(httpCon1.getInputStream());
    							}
    							else{
    								input = getAssets().open("phase512.bin");
    								bin = new BufferedInputStream(input);
    							}
    							DataInputStream dis1 = new DataInputStream(bin);
    							for(nInt = 0;nInt < 36864;nInt ++){
    								sSh = (short)(dis0.readByte() & 0xff);
    								sSh += (short)(dis0.readByte() << 8);
    								sHrtf[nInt] = sSh;
    								sSh = (short)(dis1.readByte() & 0xff);
    								sSh += (short)(dis1.readByte() << 8);
    								sPhase[nInt] = sSh;
    							}
    							bis.close();
    							bin.close();
    							dis0.close();
    							dis1.close();
    						}catch(Exception exp){
    							nMsg = -8;
    						}
    					}
    					else{
    						try{
    							input = getAssets().open("hrtf512.bin");
    							bis = new BufferedInputStream(input);
    							DataInputStream dis0 = new DataInputStream(bis);
    							input = getAssets().open("phase512.bin");
    							bin = new BufferedInputStream(input);
    							DataInputStream dis1 = new DataInputStream(bin);
    							for(nInt = 0;nInt < 36864;nInt ++){
    								sSh = (short)(dis0.readByte() & 0xff);
    								sSh += (short)(dis0.readByte() << 8);
    								sHrtf[nInt] = sSh;
    								sSh = (short)(dis1.readByte() & 0xff);
    								sSh += (short)(dis1.readByte() << 8);
    								sPhase[nInt] = sSh;
    							}
    							bis.close();
    							bin.close();
    							dis0.close();
    							dis1.close();
    						}catch(Exception exp){
    							nMsg = -8;
    						}
    					}
    		    	}
    				if(nTask == 0 && nMsg == 1){
    					isPlay = true;
    					isErr = false;
    					button0.setEnabled(false);
    					new subTask(getContext(),this).execute(1); 	// Start reproduction
    				}
    			}
/*    			if(this.getAnimation() == null)
    				this.invalidate();	*/
    			break;
    		}
    		return true;
    	}

		protected void onDraw(Canvas canvas) {
            Paint paint = new Paint();      
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE); 
            paint.setTextSize(16);   
            int nX = (int)screenWidth * 4;
            int nCenter = (int)screenWidth / 2;
            int nXconv,nAdj = 0;
            int ot = getResources().getConfiguration().orientation;
            double dDoubl;
            
    		switch(ot){
    		case Configuration.ORIENTATION_PORTRAIT:
    			nAdj = (int)((screenWidth - screenHeight) / 2);
                nCenter = (int)screenHeight / 2;
                break;
    		case Configuration.ORIENTATION_LANDSCAPE:
    			nAdj = 0;
    			nCenter = (int)screenWidth / 2;
    			break;
    		}
            if(nMsg == -1){
            	canvas.drawText("Bitmap error!", 40,40, paint);
            }
            else{
            	if(xPos < -nXwidth){
            		xPos += nXwidth;
            	}
            	else if(xPos >= nXwidth){
            		xPos -= nXwidth;
            	}
        		Rect rect = new Rect(0,0,nXwidth,nHeight);
                dDoubl = (double)xPos * dRat;
                nXconv = (int)dDoubl;
                nXconv -= nAdj;
        		
                Rect rec0 = new Rect(nXconv,0,nXconv + nX,nYconv);
                Rect rec1 = new Rect(nXconv - nX,0,nXconv,nYconv);
                Rect rec2 = new Rect(nXconv + nX,0,nXconv + nX * 2,nYconv);
                if(xPos < -(3 * nXwidth) / 4){
            		canvas.drawBitmap(bMap,rect,rec2,paint);
            		canvas.drawBitmap(bMap,rect,rec0,paint); 
            	}
            	else if(xPos > 0 && xPos < nXwidth / 4){
            		canvas.drawBitmap(bMap,rect,rec1,paint);
            		canvas.drawBitmap(bMap,rect,rec0,paint); 
            	}
            	else if(xPos <= 0){
            		canvas.drawBitmap(bMap,rect,rec0,paint);
            	}
            	else{
            		canvas.drawBitmap(bMap,rect,rec1,paint);
            	}
            	if(nMsg == 1){
            		canvas.drawText("Database is ready!",20,nYconv + 40,paint);
            	}
            	else if(nMsg == -8){
            		canvas.drawText("Database error",40,nYconv + 40,paint);
            	}
            	else if(nMsg == -9){
					button0.setEnabled(true);
            		canvas.drawText("SOPA header error!",40,nYconv + 40,paint);
            	}
            	else if(nMsg == -10){
					button0.setEnabled(true);
            		canvas.drawText("SOPA data error!",40,nYconv + 40,paint);
            	}
            	else if(nMsg == 2)
            		canvas.drawText("Sample rate "+(int)nSampleRate + " Hz",20,nYconv + 40,paint);
            	else if(nMsg == 3){
            		canvas.drawText("Sample rate "+(int)nSampleRate + " Hz",20,nYconv + 40,paint);
            		canvas.drawText("Frame size = " + (int)iSize,20,nYconv + 64,paint);
            		canvas.drawText("Tap on imageview to stop reproduction",20,nYconv + 88,paint);
            	}
            	else if(nMsg != 0){
					button0.setEnabled(true);
            		canvas.drawText((int)nParam + " bytes processed",20,nYconv + 40,paint);
            		if(nMsg == 4){
            			if(isErr){
            				showDialog(getApplicationContext(),"Oops!","Terminated because not enough data loaded");
//                			canvas.drawText("Terminated because not enough data loaded",20,nYconv + 80,paint);
            			}
            			else
            				canvas.drawText("and finished",20,nYconv + 80,paint);
            			nMsg ++;
            		}
            		else if(nMsg == 5)
            			canvas.drawText("and terminated",20,nYconv + 80,paint);
            		if(nMsg == 5 || nMsg == 4){
            			int itemNumber = adapter.getCount();
            			for(int iNum = 0;iNum < itemNumber;iNum ++){
            				if(str.equals(adapter.getItem(iNum))){
            					isAdded = true;
            					break;
            				}
            			}
            			if(!isAdded){
            				adapter.add(str);    
            			}
        				isAdded = false;
            			try{
            				writeToDB(str);
            			}catch(Exception e){
            				showDialog(getApplicationContext(),"Error!","Failed to write to database");
//            				canvas.drawText("Failed to write to database",20,nYconv + 100,paint);
            			}
            			nMsg = 6;
            		}
            	}
            	else{
                    paint.setTextAlign(Paint.Align.CENTER); 
            		canvas.drawText("Tap on imageview to start reproduction",nCenter,nYconv + 24,paint);
                    paint.setTextSize(20);
            		canvas.drawText("SOPA player for Android",nCenter,nYconv + 50,paint);
            		paint.setTextSize(28);
            		canvas.drawText("Pandroid",nCenter,nYconv + 80,paint);
            		paint.setTextSize(16);
            		canvas.drawText("Copyright (c) 2012, AIST",nCenter,nYconv + 104,paint);	
            	}
            }
    	}
    }
    public class subTask extends AsyncTask<Integer, Integer, Boolean> {
    	Context context;
		private View view;
		
    	public subTask(Context context, View view){
    		this.context = context;
    		this.view = view;
    	}
		
        @Override
        protected void onPreExecute() {
        	nTask ++;
        }
		@Override
		protected Boolean doInBackground(Integer... arg0) {
			HttpURLConnection httpCon = null;
			sopaOpen so = null;
			sopaRead sr = null;
			InputStream input = null;
			BufferedInputStream bis = null;
			URL url = null;
			int iNum;
			int nChannels = 2;
			int nBit = 16;
			int nOverlap = 4;

			try{
    			Thread.sleep(0);
    		}catch (InterruptedException e){
    			e.printStackTrace();
    		}
    		
			// Open SOPA file
			if(str.indexOf("://") != -1){
				try{
					url = new URL(str);
					httpCon = (HttpURLConnection)url.openConnection();
					httpCon.setRequestMethod("GET");
					httpCon.connect();	
					bis = new BufferedInputStream(httpCon.getInputStream());	
				}catch(IOException e){
					nMsg = -9;
					view.postInvalidate();
					return false;
				}
			}
			else{
				try{
					input = getAssets().open(str);
					bis = new BufferedInputStream(input);
	    			nMsg = 1;
				}catch(IOException e){
					nMsg = -9;
					view.postInvalidate();
					return false;
				}
			}
			so = new sopaOpen(bis);
			if(!so.readHeader()){
				nMsg = -9;
				view.postInvalidate();
				return false;
			}
			nOverlap = so.iOverlap;
			nSampleRate = so.iSampleRate;
			
			try{
				bis.close();
			}catch(IOException e){
				nMsg = -9;
				view.postInvalidate();
				return false;
			}
			if(str.indexOf("://") != -1){
				try{
					url = new URL(str);
					httpCon = (HttpURLConnection)url.openConnection();
					httpCon.setRequestMethod("GET");
					httpCon.connect();	
					bis = new BufferedInputStream(httpCon.getInputStream());	
				}catch(IOException e){
					nMsg = -9;
					view.postInvalidate();
					return false;
				}
			}
			else{
				try{
					input = getAssets().open(str);
					bis = new BufferedInputStream(input);
	    			nMsg = 1;
				}catch(IOException e){
					nMsg = -9;
					view.postInvalidate();
					return false;
				}
			}
				
    		byte[] dsByte = new byte[so.iChunkSize];
    		
    		sr = new sopaRead(bis,dsByte);
    		sr.length = so.iChunkSize;
    		sr.start();
			if(nMsg == 1)
				nMsg ++;
			view.postInvalidate();
    		
    		byte[] bRet = new byte[2];
    		final int iBYTE = nBit / 8;
    		int nBytesWritten = 0;
    		int iInt,iProc,iRem,iHlf;
    		int iRatio = 44100 / nSampleRate;
    		int iNumb,iNumImage;
			int[] iTmp = new int[EXTERNAL_BUFFER_SIZE];
			int[] nTmp = new int[EXTERNAL_BUFFER_SIZE / iBYTE];
    		double dSpL,dSpR,dSpImageL,dSpImageR;
    		double dPhaseL,dPhaseR,dPhaseImageL,dPhaseImageR;
    		
    		fft test = new fft();
    		
    		long lBuffSize = EXTERNAL_BUFFER_SIZE / iBYTE;
    		int iOff = 0;
    		int iOffset = 0;
    		long lNanoSec;
    		
    		iNum = 0;
    		
    		while(sr.iLength < lBuffSize){
    			lNanoSec = System.nanoTime();
    		}
    		if(nMsg == 2){
    			while(iNum < lBuffSize){
    				iTmp[iOff + 1] = dsByte[iNum * 4] & 0xff;
    				iTmp[iOff] = dsByte[iNum * 4 + 1] & 0xff;
					nTmp[iNum] = dsByte[iNum * 4 + 3] * 256 + (dsByte[iNum * 4 + 2] & 0x000000ff);
					if(iTmp[iOff] == 0 && iOff != 0)
					{
    					iSize = iOff * 2;		// Frame size
    					lBuffSize = iNum;
    				}
					else{
						iNum ++;
						iOff += 2;
						iOffset += 4;
					}
    			}
    		}
			
    		iProc = iSize / nOverlap;
    		iRem = iSize - iProc;
    		iHlf = iSize / 2;
    		test.iTap = iSize;
    		iRatio *= iSize / 512;
    		
    		int nSize = android.media.AudioTrack.getMinBufferSize(nSampleRate,
    				AudioFormat.CHANNEL_CONFIGURATION_STEREO,AudioFormat.ENCODING_PCM_16BIT);
    		AudioTrack aTrack = new AudioTrack(AudioManager.STREAM_MUSIC,nSampleRate,
    				AudioFormat.CHANNEL_CONFIGURATION_STEREO,AudioFormat.ENCODING_PCM_16BIT,
    				nSize * 8,AudioTrack.MODE_STREAM);
    		
    		int iFrm,iSet,iLastFrm,iFrmCnt;
    		int[][] iAngl = new int[nOverlap][iHlf + 1];
    		short[][] sData = new short[nChannels][iSize];
    		int[] abData = new int[iSize];
    		byte[] abByte = new byte[iSize * 4];
    		double dTmp;
    		double dReL[] = new double[iSize + 1];
    		double dImL[] = new double[iSize + 1];
    		double dReR[] = new double[iSize + 1];
    		double dImR[] = new double[iSize + 1];
			double[] dHann = new double[iSize];
			boolean bIsEnd = false;
			
			iLastFrm = nOverlap - 1;
			for(iInt = 0;iInt < iSize;iInt ++){		// Hanning window
				dHann[iInt] = (1 - Math.cos(2 * Math.PI * (double)iInt / (double)iSize)) / 4;	
				dImR[iInt] = 0;
			}	
			
			if(nMsg == 2){
				iOff = iSet = iFrm = 0;
				for(iNum = 0;iNum < iSize;iNum ++){
					if(iNum <= iProc){
						iAngl[iFrm][iOff] = iTmp[iSet];
						iAngl[iFrm][iOff + 1] = iTmp[iSet + 1];
						dReR[iNum] = abData[iNum] = nTmp[iNum];
					}
					else{
						iAngl[iFrm][iOff + 1] = dsByte[iOffset] & 0xff;
						iAngl[iFrm][iOff] = dsByte[iOffset + 1] & 0xff;
						abData[iNum] = dsByte[iOffset + 3] * 256 + (dsByte[iOffset + 2] & 0x000000ff);
						dReR[iNum] = abData[iNum];
					}
					iOff += 2;
					iOffset += 4;
					if(iOff == iHlf){
						iFrm ++;
						iOff = 0;
					}
					iSet += 2;
				}
				if(nMsg == 2)
					nMsg ++;	
			}
			nParam = nBytesWritten;
			view.postInvalidate();

//    		aTrack.play();
    		while(nMsg == 3){
    			if(test.fastFt(dReR,dImR,false)){
    				dReR[iSize] = dReR[0];
    				dImR[iSize] = dImR[0];
    				iAngl[0][iHlf] = 0;
    				for(iNum = 0;iNum < iHlf;iNum ++){
    					int nImage = iSize - iNum;
    					int iFreq = iNum / iRatio;
    					if(iAngl[0][iNum] == 0 || iFreq == 0 || iAngl[0][iNum] == 255){
    						dSpR = dSpL = dReR[iNum];
    						dSpImageL = dSpImageR = dReR[nImage];
    						dPhaseL = dPhaseR = dImR[iNum];
    						dPhaseImageL = dPhaseImageR = dImR[nImage];
    					}
    					else{
    						iAngl[0][iNum] += (int)dRot;	// Control panning
    						iAngl[0][iNum] -= 1;
    						if(iAngl[0][iNum] > 71)
    							iAngl[0][iNum] -= 72;
    						else if(iAngl[0][iNum] < 0)
    							iAngl[0][iNum] += 72;
    						iNumb = 512 * (72 - iAngl[0][iNum]) + iFreq;
    						iNumImage = 512 * (72 - iAngl[0][iNum]) + 512 - iFreq;
    						if(iNumImage >= 36864)
    							iNumImage -= 36864;
    						else if(iNumImage < 0)
    							iNumImage += 36864;
    						if(iNumb >= 36864)
    							iNumb -= 36864;
    						else if(iNumb < 0)
    							iNumb += 36864;
    						dTmp = (double)sHrtf[iNumb];
    						dSpL = dReR[iNum] * dTmp / 2048;

    						dTmp = (double)sPhase[iNumb];
    						dPhaseL = dImR[iNum] + dTmp / 10000;
    								
    						dTmp = (double)sHrtf[iNumImage];
    						dSpImageL = dReR[nImage] * dTmp / 2048;
    								
    						dTmp = (double)sPhase[iNumImage];
    						dPhaseImageL = dImR[nImage] + dTmp / 10000;
    								
    						iNumb = 512 * iAngl[0][iNum] + iFreq;
    						iNumImage = 512 * iAngl[0][iNum] + 512 - iFreq;
    						if(iNumImage >= 36864)
    							iNumImage -= 36864;
    						else if(iNumImage < 0)
    							iNumImage += 36864;
    						if(iNumb >= 36864)
    							iNumb -= 36864;
    						else if(iNumb < 0)
    							iNumb += 36864;
    						dTmp = (double)sHrtf[iNumb];
    						dSpR = dReR[iNum] * dTmp / 2048;
    								
    						dTmp = (double)sPhase[iNumb];
    						dPhaseR = dImR[iNum] + dTmp / 10000;
    								
    						dTmp = (double)sHrtf[iNumImage];
    						dSpImageR = dReR[nImage] * dTmp / 2048;

    						dTmp = (double)sPhase[iNumImage];
    						dPhaseImageR = dImR[nImage] + dTmp / 10000;
    					}
    					dReL[iNum] = dSpL * Math.cos(dPhaseL);
    					dReR[iNum] = dSpR * Math.cos(dPhaseR);
    					dImL[iNum] = dSpL * Math.sin(dPhaseL);
    					dImR[iNum] = dSpR * Math.sin(dPhaseR);
    					dReL[nImage] = dSpImageL * Math.cos(dPhaseImageL);
    					dReR[nImage] = dSpImageR * Math.cos(dPhaseImageR);
    					dImL[nImage] = dSpImageL * Math.sin(dPhaseImageL);
    					dImR[nImage] = dSpImageR * Math.sin(dPhaseImageR);
    				}
    				dReL[iHlf] = dReR[iHlf];
    				dImL[iHlf] = dImR[iHlf];
    				if(test.fastFt(dReL,dImL,true)){
    					if(test.fastFt(dReR,dImR,true)){
    						for(iNum = 0;iNum < iSize;iNum ++){
    							// Hanning window
    							dReL[iNum] *= dHann[iNum];
    							dReR[iNum] *= dHann[iNum];
    							sData[0][iNum] += dReL[iNum];
    							sData[1][iNum] += dReR[iNum];
    						}
    					}
    				}
    				bRet[0] = bRet[1] = 0;
    				for(iNum = 0;iNum < iProc;iNum ++){
    					iOff = iNum * 4;
    					intToByte(sData[0][iNum],bRet);
    					abByte[iOff] = bRet[0];
    					abByte[iOff + 1] = bRet[1];
    					intToByte(sData[1][iNum],bRet);
    					abByte[iOff + 2] = bRet[0];
    					abByte[iOff + 3] = bRet[1];
    				}
    				for(iInt = 0;iInt < iSize;iInt ++){
    					if(iInt < iRem){
    						sData[0][iInt] = sData[0][iInt + iProc];
    						sData[1][iInt] = sData[1][iInt + iProc];
    						dReR[iInt] = abData[iInt] = abData[iInt + iProc];
    					}
    					else{
    						sData[0][iInt] = sData[1][iInt] = 0;
//    						abData[iInt] = 0;
    					}
						dImR[iInt] = 0;
    				}
    				for(iFrmCnt = 0;iFrmCnt < nOverlap - 1;iFrmCnt ++){
    					for(iInt = 0;iInt < iHlf;iInt ++){
    						iAngl[iFrmCnt][iInt] = iAngl[iFrmCnt + 1][iInt];
    					}
    				}
    				nBytesWritten += aTrack.write(abByte,0,iProc * iBYTE * nChannels);
    			}
    			if(nBytesWritten == nSize * 8){
    	    		aTrack.play();
    	    		isStarted = true;
    			}
    			if(bIsEnd || isErr){
    				break;
    			}
    			iOff = 0;
    			for(iNum = 0;iNum < iProc;iNum ++){
    				if(bIsEnd){
    					iAngl[iLastFrm][iOff] = iAngl[iLastFrm][iOff + 1] = 1;
    					dReR[iRem + iNum] = abData[iRem + iNum] = 0;
    				}
    				else if(iOffset >= sr.length + iRem * 4){
    					iAngl[iLastFrm][iOff + 1] = iAngl[iLastFrm][iOff] = 1;
    					dReR[iRem + iNum] = abData[iRem + iNum] = 0;
    					sr.iLength += 4;
    					bIsEnd = true;
//    					nMsg ++;
    				}
    				else if(nBytesWritten < sr.iLength){
    					if(iOffset >= sr.length){
        					iAngl[iLastFrm][iOff + 1] = iAngl[iLastFrm][iOff] = 1;
        					dReR[iRem + iNum] = abData[iRem + iNum] = 0;
        					sr.iLength += 4;
    					}
    					else{
    						iAngl[iLastFrm][iOff + 1] = (int)dsByte[iOffset] & 0xff;
    						iAngl[iLastFrm][iOff] = (int)dsByte[iOffset + 1] & 0xff;
    						abData[iRem + iNum] = dsByte[iOffset + 3] * 256 + (dsByte[iOffset + 2] & 0x000000ff);
    						dReR[iRem + iNum] = abData[iRem + iNum];
    					}
    				}
    				else{
    					isErr = true;
    				}
    				iOff += 2;
    				iOffset += 4;
    			}
    			nParam = nBytesWritten;
//    			view.postInvalidate();
    		}
    		sr.isTerminated = true;
    		while(!sr.isFinished)
    			lNanoSec = System.nanoTime();
    		try{
    			bis.close();
    			if(input != null)
    				input.close();
    		}catch(IOException e){
    			nMsg = -10;	
    		}catch(Exception e){
    			nMsg = -10;	
    		}
    		if(isStarted){
    			aTrack.stop();
    			aTrack.release();
    		}
    		nMsg ++;
    		view.postInvalidate();
    		if(nMsg > 2)
    			return true;
    		else
    			return false;
    	}
		protected void onPostExecute(Boolean result) {
    		nTask --;
    	} 
    }
	private static void intToByte(int iDt,byte bRet[])
	{
		bRet[0] = (byte)(iDt & 0x000000ff);
		bRet[1] = (byte)(iDt >>> 8 & 0x000000ff);
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3){
		isSpinner = true;
		if(button0.isEnabled())
			button0.performClick();
	}
	public void onNothingSelected(AdapterView<?> arg0){
		isSpinner = false;
	}
	
	private String readFromDB(int i)throws Exception{
		Cursor cursor = dbObject.query(
				DB_TABLE, new String[]{"id","info"},
				"id="+i,null,null,null,null);
		if(cursor.getCount()==0)throw new Exception();
		cursor.moveToFirst();
		String valueCursor = cursor.getString(1);
		cursor.close();
		return valueCursor;
	}
	
	private void writeToDB(String editedStr)throws Exception{
		int iColumnNum,iNum = 0;
		Cursor cursor = dbObject.query(
				DB_TABLE, new String[]{"id","info"},
				null,null,null,null,null);
		iColumnNum = cursor.getCount();
		ContentValues contentValObject = new ContentValues();
		if(iColumnNum != 0){
			cursor.moveToFirst();
			iNum = 0;
			do{
				String itemStr = cursor.getString(1);
				if(itemStr.equals(editedStr)){
					iColumnNum --;
					while(cursor.moveToNext()){
						contentValObject.put("id","" + iNum);
						contentValObject.put("info",cursor.getString(1));
						dbObject.update(DB_TABLE,contentValObject,"id=?",new String[]{""+iNum});
						iNum ++;
					}
					dbObject.delete(DB_TABLE,"id=" + iColumnNum, null);
					break;
				}
				iNum ++;
			}while(cursor.moveToNext());
		}
		if(iColumnNum >= MaxColumn){
			iNum = 0;
			cursor.moveToFirst();
			do{
				cursor.moveToPosition(iNum + 1);
				contentValObject.put("info", cursor.getString(1));
				dbObject.update(DB_TABLE,contentValObject,"id=?",new String[]{"" + iNum});
				iNum ++;
			}while(cursor.moveToNext());
		}
		if(iColumnNum == 0){
			contentValObject.put("id","0");
			contentValObject.put("info",editedStr);
			dbObject.insert(DB_TABLE,null,contentValObject);
		}
		else if(MaxColumn > iColumnNum){
			cursor.moveToPosition(iColumnNum);
			contentValObject.put("id","" + iColumnNum);
			contentValObject.put("info",editedStr);
			dbObject.insert(DB_TABLE,null,contentValObject);
		}
		else{
			iNum = MaxColumn - 1;
			contentValObject.put("id","" + iNum);
			contentValObject.put("info",editedStr);
			dbObject.update(DB_TABLE,contentValObject,"id=?",new String[]{"" + iNum});
		}

		cursor.close();
//		dbObject.close();
	}

	@Override
    protected void onDestroy() {
    	nMsg ++;
    	super.onDestroy();
    }
}