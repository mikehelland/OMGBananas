package com.monadpad.omgbananas;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class MonadJam {

    private Random rand = new Random();

    private int subbeats = 4;
    private int beats = 8;
    private int subbeatLength = 125; //70 + rand.nextInt(125); // 125;

    private DrumChannel drumChannel;

    private Channel basslineChannel;

    private Channel keyboardChannel;
    private Channel guitarChannel;
    private DrumChannel samplerChannel;

    private SoundPool pool = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);
    private boolean soundPoolLoaded = false;

    private Context mContext;

    PlaybackThread playbackThread;

    boolean cancel = false;

    private MelodyMaker mm;

    private boolean chordsEnabled = true;

    private boolean playing = false;

    private int progressionI = -1;

    private ArrayList<View> viewsToInvalidateOnBeat = new ArrayList<View>();
    private ArrayList<View> viewsToInvalidateOnNewMeasure = new ArrayList<View>();

    private int currentChord = 0;

    public MonadJam(Context context) {

        mContext = context;
        mm = new MelodyMaker(mContext);
        mm.makeMotif();
        mm.makeMelodyFromMotif(beats);

        setDrumset(0);

    }

    public void makeChannels() {

        drumChannel = new HipDrumChannel(mContext, pool, this);
        basslineChannel = new BassSamplerChannel(mContext, pool);
        guitarChannel = new ElectricSamplerChannel(mContext, pool);
        samplerChannel = new SamplerChannel(mContext, pool, this);

        keyboardChannel = new KeyboardSamplerChannel(mContext, pool);

        if (Build.VERSION.SDK_INT >= 11) {
            pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                int loadedSounds = 0;
                @Override
                public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                    loadedSounds++;
                    Log.d("MGH sound pool", Integer.toString(loadedSounds));

                    if (loadedSounds == 70) {
                        soundPoolLoaded = true;
                        Log.d("MGH sound pool", "loaded");
                    }
                }
            });
        }

        drumChannel.loadPool();
        basslineChannel.loadPool();
        guitarChannel.loadPool();
        samplerChannel.loadPool();
        keyboardChannel.loadPool();
    }



    public void playBeatSampler(int subbeat) {

        if (subbeat == 0) {
            basslineChannel.resetI();
            guitarChannel.resetI();
            keyboardChannel.resetI();
        }

        drumChannel.playBeat(subbeat);
        samplerChannel.playBeat(subbeat);

        double beat = subbeat / (double)subbeats;
        playChannelBeat(basslineChannel, beat);

        playChannelBeat(guitarChannel, beat);

        playChannelBeat(keyboardChannel, beat);

    }

    public ArrayList<Note> makeChannelNotes(Channel channel, int transpose) {

        ArrayList<Note> notes = mm.getCurrentMelody();

        if (rand.nextInt(5) == 0) {
            mm.makeMotif();
            mm.makeMelodyFromMotif(beats);
        }
        else if (rand.nextInt(3) == 0) {
            // keep the motif
            mm.makeMelodyFromMotif(beats);
        }

        notes = mm.applyScale(notes, currentChord, transpose);
        channel.setNotes(notes);
        channel.resetI();

        return notes;

    }


    public void kickIt() {

        if (!playing) {
            cancel = false;
            playbackThread = new PlaybackThread();
            playbackThread.start();
        }

    }

    public boolean  toggleMuteBassline() {
        basslineChannel.toggleEnabled();
        return basslineChannel.enabled;

    }
    public boolean  toggleMuteGuitar() {
        guitarChannel.toggleEnabled();
        return guitarChannel.enabled;
    }


    public boolean toggleMuteRhythm() {
        keyboardChannel.toggleEnabled();
        return keyboardChannel.enabled;
    }

    public boolean toggleMuteDrums() {
        drumChannel.toggleEnabled();
        return drumChannel.enabled;
    }

    public int getCurrentSubbeat() {
        int i = playbackThread.i;
        if (i == 0) i = beats * subbeats;
        return i - 1;

    }

    class PlaybackThread extends Thread {

        int i;
        int lastI;

        public void run() {

            playing = true;
            progressionI = -1; // gets incremented by onNewLoop
            onNewLoop();

            long lastBeatPlayed = 0;
            long now;
            long timeSinceLast;

            i = 0;


            while (!cancel) {

                now = System.currentTimeMillis();
                timeSinceLast = now - lastBeatPlayed;

                if (timeSinceLast < subbeatLength) {
                    pollFinishedNotes(now);
                    continue;
                }

                lastBeatPlayed = now;
                playBeatSampler(i);

                lastI = i++;

                if (i == beats * subbeats) {
                    i = 0;
                    onNewLoop();

                    for (View iv : viewsToInvalidateOnNewMeasure) {
                        iv.postInvalidate();
                    }

                }

                for (View iv : viewsToInvalidateOnBeat) {
                    iv.postInvalidate();
                }

            }

            playing = false;

        }

        public void pollFinishedNotes(long now) {
            long finishAt = basslineChannel.getFinishAt();
            if (finishAt > 0 && now >= finishAt) {
                basslineChannel.mute();
            }
            finishAt = keyboardChannel.getFinishAt();
            if (finishAt > 0 && now >= finishAt) {
                keyboardChannel.mute();
            }

        }

    }

    private int[] progression = {0};

    void onNewLoop() {

        //long now = System.currentTimeMillis();
        //if (lastHumanInteraction > now - 30000) {
        //    randomRuleChange(now);
        //}

        if (chordsEnabled)
            progressionI++;

        if (progressionI >= progression.length || progressionI < 0) {
            progressionI = 0;
        }
        int p = progression[progressionI];

        basslineChannel.setNotes(mm.applyScale(basslineChannel.getNotes().list, p, 0));
        guitarChannel.setNotes(mm.applyScale(guitarChannel.getNotes().list, p, 12));
        keyboardChannel.setNotes(mm.applyScale(keyboardChannel.getNotes().list, p, 24));

    }

    public void finish() {
        cancel = true;
        keyboardChannel.mute();
        basslineChannel.mute();
        guitarChannel.mute();
        pool.release();
        pool = null;
    }


    public void randomRuleChange(long now) {

        int change = rand.nextInt(7);

        switch (change) {
            case 0:
                //subbeatLength += rand.nextBoolean() ? 10 : -10;
                subbeatLength = 70 + rand.nextInt(125); // 125
                break;
            case 1:
                mm.pickRandomKey();
                break;
            case 2:
                mm.pickRandomScale();
                break;
            case 3:
                makeChordProgression();
                break;
            case 4:
                makeDrumbeatFromMelody();

                break;
            case 5:
                makeChannelNotes(basslineChannel, 0);

                break;
            case 6:
                makeChannelNotes(keyboardChannel, 12);

                break;

        }

    }

    public void everyRuleChange() {

        subbeatLength = 70 + rand.nextInt(125);

        mm.pickRandomKey();
        mm.pickRandomScale();
        makeChordProgression();

        mm.makeMotif();
        mm.makeMelodyFromMotif(beats);

        // effects probabilities of makeDrumBeats
        setDrumset(rand.nextInt(2));

        monkeyWithBass();
        monkeyWithDrums();
        monkeyWithGuitar();
        monkeyWithSynth();

        makeChannelNotes(guitarChannel, 12);
        makeChannelNotes(keyboardChannel, 12);


        playbackThread.i = 0;

        drumChannel.enable();
        basslineChannel.enable();
        keyboardChannel.enable();
        //guitarChannel.enable();
    }


    public String getKeyName() {
        return mm.getKeyName();
    }

    public void makeChordProgression() {
        progressionI = 0;

        int pattern = rand.nextInt(10);
        int scaleLength = mm.getScaleLength();

        if (pattern < 2) {
            int rc =  2 + rand.nextInt(scaleLength - 2);
            progression = new int[] {0, 0, rc, rc};
        }
        else if (pattern < 4)
            progression = new int[] {0,
                                    rand.nextInt(scaleLength),
                                    rand.nextInt(scaleLength),
                                    rand.nextInt(scaleLength)};

        else if (pattern < 6) {
            if (scaleLength == 6)
                progression = new int[] {0, 4, 5, 2};
            else if (scaleLength == 5) {
                progression = new int[] {0, 3, 4, 2};
            }
            else {
                progression = new int[] {0, 4, 5, 3};
            }
        }

        else if (pattern < 8) {
            if (scaleLength == 6)
                progression = new int[] {0, 0, 2, 0, 4, 2, 0, 4};
            else if (scaleLength == 5) {
                progression = new int[] {3, 4, 2, 0, };
            }
            else {
                progression = new int[] {0, 0, 3, 0, 4, 3, 0, 4};
            }
        }

        else if (pattern == 8)
            progression = new int[] {rand.nextInt(scaleLength), rand.nextInt(scaleLength),
                    rand.nextInt(scaleLength)};

        else {
            int changes = 1 + rand.nextInt(8);
            progression = new int[changes];
            for (int i = 0; i < changes; i++) {
                progression[i] = rand.nextInt(scaleLength);
            }
        }

        currentChord = progression[0];

    }

    public int getBPM() {
        return 60000 / (subbeatLength * subbeats);
    }

    public boolean isPlaying() {
        return playing;
    }

    public String getData() {

        StringBuilder sb = new StringBuilder();

        sb.append("{\"type\": \"SECTION\", \"scale\": \"");
        sb.append(mm.getScale());
        sb.append("\", \"key\": ");
        sb.append(mm.getKey());
        sb.append(", \"beats\" :");
        sb.append(beats);
        sb.append(", \"subbeats\" :");
        sb.append(subbeats);
        sb.append(", \"subbeatMillis\" :");
        sb.append(subbeatLength);

        sb.append(", \"data\" : [");

        drumChannel.getData(sb);

        sb.append(", ");

        getBasslineData(sb);

        sb.append(", ");

        getSynthData(sb);

        sb.append(", ");

        getChordsData(sb);

        sb.append("]}");

        return sb.toString();

    }


    public void getBasslineData(StringBuilder sb) {

        sb.append("{\"type\" : \"BASSLINE\", \"sound\": \"PRESET_BASS\", \"scale\": \"");
        sb.append(mm.getScale());
        sb.append("\", \"key\": ");
        sb.append(mm.getKey());
        sb.append(", \"notes\" : [");

        boolean first = true;
        for (Note note : basslineChannel.getNotes().list) {

            if (first)
                first = false;
            else
                sb.append(", ");

            sb.append("{\"rest\": ");
            sb.append(note.isRest());
            sb.append(", \"beats\": ");
            sb.append(note.getBeats());
            if (!note.isRest()) {
                sb.append(", \"scaledNote\" :");
                sb.append(note.getNote());
                sb.append(", \"note\" :");
                sb.append(note.getNakedNote());
            }
            sb.append("}");
        }
        sb.append("]}");

    }

    public void getSynthData(StringBuilder sb) {

        sb.append("{\"type\" : \"MELODY\", \"sound\": \"PRESET_SYNTH1\", \"scale\": \"");
        sb.append(mm.getScale());
        sb.append("\", \"key\": ");
        sb.append(mm.getKey());
        sb.append(", \"notes\" : [");

        boolean first = true;

        for (Note note : keyboardChannel.getNotes().list) {
            if (first)
                first = false;
            else
                sb.append(", ");

            sb.append("{\"rest\": ");
            sb.append(note.isRest());
            sb.append(", \"beats\": ");
            sb.append(note.getBeats());
            if (!note.isRest()) {
                sb.append(", \"scaledNote\" :");
                sb.append(note.getNote());
                sb.append(", \"note\" :");
                sb.append(note.getNakedNote());
            }
            sb.append("}");
        }
        sb.append("]}");

    }

    public void getChordsData(StringBuilder sb) {

        sb.append("{\"type\" : \"CHORDPROGRESSION\", \"data\" : [");

        boolean first = true;

        for (int chord : progression) {
            if (first)
                first = false;
            else
                sb.append(", ");

            sb.append(chord);
        }
        sb.append("]}");

    }

    public void monkeyWithEverything() {
        everyRuleChange();

    }

    public void monkeyWithDrums() {

        //half the time, drums from the bass
        if (rand.nextBoolean()) {
            drumChannel.makeDrumBeatsFromMelody(basslineChannel.getNotes().list);
        }
        else {
            // make them separate
            drumChannel.makeDrumBeats();
        }

    }

    public void monkeyWithBass() {
        makeChannelNotes(basslineChannel, 0);
    }

    public void monkeyWithSynth() {
        makeChannelNotes(keyboardChannel, 12);
    }

    public void monkeyWithGuitar() {
        makeChannelNotes(guitarChannel, 12);
    }

    public void monkeyWithChords() {

        makeChordProgression();
        //progressionI = 0;
    }


    public int[] getProgression() {
        return progression;
    }

    public int getChordInProgression() {
        return progressionI;
    }

    public int[] getScale() {
        return mm.getScaleArray();
    }

    public int getScaleIndex() {
        return mm.getScaleIndex();
    }

    public Channel getGuitarChannel() {
        return guitarChannel;
    }

    public Channel getBassChannel() {
        return basslineChannel;
    }

    public Channel getSynthChannel() {
        return keyboardChannel;
    }

    public int getKey() {
        return mm.getKey() % 12;
    }

    public void setDrumset(int set) {
        //drumset = set;
        //setCaptions();
    }

    public void addInvalidateOnBeatListener(View view) {
        viewsToInvalidateOnBeat.add(view);
    }

    public void addInvalidateOnNewMeasureListener(View view) {
        viewsToInvalidateOnNewMeasure.add(view);
    }


    public void removeInvalidateOnBeatListener(View view)  {
        viewsToInvalidateOnBeat.remove(view);
    }

    public DrumChannel getSamplerChannel() {
        return samplerChannel;
    }

    public DrumChannel getDrumChannel() {
        return drumChannel;
    }

    public void setScale(int scaleI) {
        mm.setScale(scaleI);
    }
    public void setKey(int keyI) {
        mm.setKey(keyI);
    }

    private void playChannelBeat(Channel channel, double beat) {

        if (channel.getState() != Channel.STATE_PLAYBACK)
            return;

        int i = channel.getI();
        if (i <  channel.getNotes().list.size()) {
            Note note = channel.getNotes().list.get(i);
            if (note.getBeatPosition() == beat) {

                channel.playNote(note);
                channel.finishCurrentNoteAt(System.currentTimeMillis() +
                        (long)(note.getBeats() * 4 * subbeatLength) - 50);
                channel.bumpI();

            }
        }
    }

    public void setBPM(float bpm) {
        subbeatLength = (int)((60000 / bpm) / subbeats);
        //bpm = 60000 / (subbeatLength * subbeats);
    }

    public void setChordProgression(int[] chordProgression) {
        progressionI = 0;
        progression = chordProgression;
        currentChord = progression[0];
    }

    public int getBeats() {
        return beats;
    }

    public int getSubbeats() {
        return subbeats;
    }

    public Random getRand() {
        return rand;
    }

    public void makeDrumbeatFromMelody() {
        drumChannel.makeDrumBeatsFromMelody(basslineChannel.getNotes().list);
    }
}