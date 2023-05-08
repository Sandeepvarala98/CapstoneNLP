package com.example.speechtoimage
import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.backup.BackupManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.ColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.*
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import java.io.*
import java.util.*
import java.util.regex.Pattern
//enum class called Mode with three possible values: DEMO, PARENT, and CHILD.
enum class Mode {
    DEMO, PARENT, CHILD
}

// Declare an enum class called Mand
enum class Mand {
    MAND1, // First mandatory option
    MAND2, // Second mandatory option
    MAND3, // Third mandatory option
    SETMENU, // Option to set a menu
    SETLOCK // Option to set a lock
}



@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(),GestureDetector.OnGestureListener {
    lateinit var outputTV: TextView
    lateinit var micIV: ImageView // this is deprecated
    lateinit var dbHelper: DatabaseHelper // database handler
    private var speechRecognizer: SpeechRecognizer? = null // speech recognizer handler
    private lateinit var animationView: LottieAnimationView // handler for animation lottie
    private lateinit var mediaPlayer: MediaPlayer // this is deprecated not being used
    private var currentMode: Mode = Mode.DEMO
    private var currentMand: Mand = Mand.MAND1
    private lateinit var gestureDetector: GestureDetector // handler for gesture
    private var tapCount: Int = 0 // to count the no of touches on screen
    private lateinit var textToSpeech: TextToSpeech // handler for text to speech
    // colors constraints to change the color
    object ColorConstants {
        val transparent = "#00000000"
        val black = "#ff000000"
        val dark_gray = "#ff444444"
        val gray = "#ff888888"
        val light_gray = "#ffcccccc"
        val white = "#ffffffff"
        val red = "#ffff0000"
        val green = "#ff00ff00"
        val blue = "#ff0000ff"
        val yellow = "#ffffff00"
        val cyan = "#ff00ffff"
        val magenta = "#ffff00ff"
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backupManager = BackupManager(this)
        backupManager.dataChanged() // this is used for making backup of the data when app is uninstalled
        gestureDetector = GestureDetector(this, this)
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.drum)
        // database  helper for manipulate the database
        dbHelper = DatabaseHelper(this)
        dbHelper.SaveMenu("menu","show me menu") // initial menu set
        dbHelper.SaveMenu("lock","close the app") // initial lock set
        addData()  // adding data
        initializeTextToSpeech() // text to speech initialization


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // keep screen awake
        parent() // first time parent screen


    }

    // Method that has the functionality to add images to Database
    //It reads the images from the resources folder and inserts into database by calling saveData()
    //method of DatabaseHelper class
    private fun addData(){
        val myList_adj = listOf(
            "/new_sound_json/ADJECTIVES/GREEN/green.json", "/new_sound_json/ADJECTIVES/BLUE/blue.json", "/new_sound_json/ADJECTIVES/RED/red.json", "/new_sound_json/ADJECTIVES/ANGRY/angry.json", "/new_sound_json/ADJECTIVES/HOT/hot.json", "/new_sound_json/NOUNS/CAT/cat.json", "/new_sound_json/NOUNS/CAR/car.json", "/new_sound_json/NOUNS/DOG/dog.json", "/new_sound_json/NOUNS/BEE/bee.json", "/new_sound_json/NOUNS/DRUM/drum.json", "/new_sound_json/NOUNS/BELL/bell.json", "/new_sound_json/NOUNS/HORN/horn.json", "/new_sound_json/NOUNS/COW/cow.json", "/new_sound_json/NOUNS/CAKE/cake.json", "/new_sound_json/NOUNS/LION/lion.json", "/new_sound_json/VERBS/RUNNING/running.json", "/new_sound_json/VERBS/DANCING/dancing.json", "/new_sound_json/VERBS/SLEEPING/sleeping.json", "/new_sound_json/VERBS/SINGING/singing.json", "/new_sound_json/VERBS/CLAPPING/clapping.json", "/new_sound_json/VERBS/CLIMBING/climbing.json", "/new_sound_json/VERBS/SWIMMING/swimming.json", "/new_sound_json/VERBS/EATING/eating.json", "/new_sound_json/VERBS/CUTTING/cutting.json","/new_sound_json/VERBS/LAUGHING/laughing.json","/new_sound_json/VERBS/WALKING/walking.json",
            "/colors/BROWN/BROWN.json", "/colors/GRAY/GRAY.json", "/colors/PINK/PINK.json", "/colors/PURPLE/PURPLE.json", "/colors/YELLOW/YELLOW.json", "/colors/BLACK/BLACK.json", "/colors/ORANGE/ORANGE.json"
        )

        val types = listOf(
            "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS","VERBS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS","ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "ADJECTIVES","ADJECTIVES", "ADJECTIVES"
        )
        for( (item, type) in myList_adj.zip(types)) {
            val image = item.split("/").last()
            val image_name = image.split(".").first().split('/').last().replace("_"," ")
            try{
                Log.e("MM:", "data: $item")
                val inputStream = this.javaClass.getResourceAsStream(item)

                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = bufferedReader.use { it.readText() }

                Log.e("color",image_name.toLowerCase())
                dbHelper.SaveData(image_name.toLowerCase(),type,jsonString)
            }
            catch (e: Exception) {
                Log.e("error:", "Error writing file: ${e.message}")
            }
        }
    }
    // This method is called when you click any one of the buttons on the parent menu scrren
    @SuppressLint("SetTextI18n")
    private fun child(){
        setContentView(R.layout.activity_main_child)// set view for user

        outputTV = findViewById(R.id.idTVOutput)
        if (currentMand==Mand.SETMENU) // when you click on set menu
            outputTV.text = "Speak to setup new menu command"
        if (currentMand==Mand.SETLOCK) // when you click on set lock
            outputTV.text = "Speak to setup new lock command"
        micIV = findViewById(R.id.idIVMic)

//
        checkAudioPermission() // audio permission
        // checking permission if not then ask for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        micIV.setOnClickListener {
            micIV.setColorFilter(ContextCompat.getColor(this, R.color.mic_enabled_color)) // #FF0E87E7
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            startSpeechToText()
        }

        micIV.setColorFilter(ContextCompat.getColor(this, R.color.mic_enabled_color)) // #FF0E87E7
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        startSpeechToText() // start speech when you click on mand
        //testing()

        //println(findNouns("Dog cat elephant"))
    }
    // to get regex match for menu command, to check if the sppech contains the menu command
    private fun GetPatern(menu_command:String,input_text:String): Boolean {
        val regexPattern = menu_command.toRegex()
        return regexPattern.containsMatchIn(input_text)
    }
    // color code matching
    private fun getColorCodeByName(colorName: String): String {
        return when (colorName.toLowerCase()) {
            "transparent" -> ColorConstants.transparent
            "black" -> ColorConstants.black
            "dark gray" -> ColorConstants.dark_gray
            "gray" -> ColorConstants.gray
            "light gray" -> ColorConstants.light_gray
            "white" -> ColorConstants.white
            "red" -> ColorConstants.red
            "green" -> ColorConstants.green
            "blue" -> ColorConstants.blue
            "yellow" -> ColorConstants.yellow
            "cyan" -> ColorConstants.cyan
            "magenta" -> ColorConstants.magenta
            else -> "null"
        }
    }
    // color the object
    private fun color_anim(color_:String){

        val yourColor = Color.parseColor(color_)
        val filter = SimpleColorFilter(yourColor)
        val keyPath = KeyPath("**")
        // this is lottie callback to assign color
        val callback: LottieValueCallback<ColorFilter> = LottieValueCallback(filter)

        animationView.addValueCallback(
            keyPath,
            LottieProperty.COLOR_FILTER,
            callback,
        )
        animationView.playAnimation()



    }
    // To increase the object size
    private fun bigsize()
    {
        val startScale = 1.5f
        val endScale = 1.5f

        val scaleAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = 10000
            repeatCount = 2
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {

                val value = it.animatedValue as Float
                animationView.scaleX = value
                animationView.scaleY = value
            }
        }

        scaleAnimator.start() // start the animation

    }
    // To decrease the object size
    private fun smallsize()
    {
        val startScale = 0.5f // size of the object to scale
        val endScale = 0.5f

        val scaleAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = 10000
            repeatCount = 2
            repeatMode = ValueAnimator.REVERSE // type of mode
            addUpdateListener {

                val value = it.animatedValue as Float
                animationView.scaleX = value
                animationView.scaleY = value
            }
        }

        scaleAnimator.start()

    }
    // To rotate the object
    private fun rotate(){
        val rotateDuration = resources.getInteger(R.integer.rotate_duration) // rotate duration for object

        val rotateAnimator = ObjectAnimator.ofFloat(animationView, "rotation", 0f, 360f).apply {
            duration = rotateDuration.toLong()
            repeatCount = 5 // how many times you want to rotate
            repeatMode = ValueAnimator.RESTART // it will always restart
        }

        rotateAnimator.start()
    }
    // To jump the object
    private fun jump(){
        val jumpHeight = 220f // how much jump you want to have for object
        val jumpDuration = 1000 // for how much time you can see


        val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = jumpDuration.toLong()
            repeatCount = 5 // 5 times will repeat
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val translationY = -jumpHeight * fraction * (fraction - 2) // translating y values with this formula
                animationView.translationY = translationY
            }
        }

        valueAnimator.start()

    }
    // putting the lottie animation image into the app
    @SuppressLint("RestrictedApi")
    private fun setAnimation(image: String, input:String, input2:String){
        try {

            animationView = findViewById(R.id.animation_view)

            LottieComposition.Factory.fromJsonString(image
            ) { composition ->

                if (composition != null) {
                    animationView.setComposition(composition) // putting into the composition then it will play the animation

                    animationView.playAnimation()

                }
            }

            // condition to check which operation(rotate, small, big,..) should perform

            if (input == "rotate" || input2 == "rotate")
                rotate()
            else if (input == "jumping" || input2 == "jumping") // when jumping
                jump()
            else if (input == "small" || input2 == "small") // when small
                smallsize()
            else if (input == "big" || input2 == "big") // when we get big in result
                bigsize()
            // when there is nothing we can check is there any color is being spoken

            if (input == "none" && input2 != "none") {

                val color = getColorCodeByName(input2)
                if (color != "null")
                    color_anim(color)
            }else if(input != "none" && input2 == "none")
            {

                val color = getColorCodeByName(input)
                if (color != "null")
                    color_anim(color)
            }
            else{
                val color = getColorCodeByName(input2)

                if (color != "null")
                    color_anim(color)
                val color1 = getColorCodeByName(input)
                if (color1 != "null")
                    color_anim(color1)
            }



        } catch (e: Exception) {
            Log.e("set:", "Error in set animation: ${e.message}")
        }
    }
    // parent screen which will pop up when we open the app
    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    private fun parent(){
        setContentView(R.layout.activity_main_parent)
        val toggleSwitch: Switch = findViewById(R.id.toggle_switch) // switch for demo
        if (currentMode==Mode.DEMO)
            toggleSwitch.isChecked = true

        currentMode = Mode.PARENT

        val mand_1 = findViewById<Button>(R.id.btn1Mand) // mand 1 button
        val mand_2 = findViewById<Button>(R.id.btn2Mand) // mand 2 button
        val mand_3 = findViewById<Button>(R.id.btn3Mand) // mand 3 button
        val setmenu = findViewById<Button>(R.id.btnsetmenu) // menu button
        val setlock = findViewById<Button>(R.id.btnsetlock) // lock command


        setlock.setOnClickListener{
            currentMand = Mand.SETLOCK // click on lock button
            child()
        }
        setmenu.setOnClickListener{
            currentMand = Mand.SETMENU // click on menu button
            child()
        }
        mand_1.setOnClickListener{
            currentMand = Mand.MAND1 // click on mand1 button
            if (toggleSwitch.isChecked)
                currentMode = Mode.DEMO

            child()

        }
        mand_2.setOnClickListener{
            currentMand = Mand.MAND2 // click on mand2 button
            if (toggleSwitch.isChecked)
                currentMode = Mode.DEMO

            child()
        }

        mand_3.setOnClickListener{
            currentMand = Mand.MAND3 // click on mand3 button
            if (toggleSwitch.isChecked)
                currentMode = Mode.DEMO

            child()
        }

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentMode = Mode.DEMO // when click on toggle
            } else {
                currentMode = Mode.CHILD
            }
            child()
        }

    }


// in demo mode speaking
    private fun speak(text: String) {
        if (text.isNotEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
        }
    }
    // this method used to initialized text to speech package
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, object : OnInitListener {
            override fun onInit(status: Int) {
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(
                            this@MainActivity,
                            "Language not supported",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to initialize TextToSpeech",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }


    var count:Int = 1
    // this method is used for demo mode
//    Sets the content view to the layout activity_main_demo.
//    Initializes outputTV to the TextView with ID idTVOutput.
//    Gets a reference to the SQLite database using dbHelper and creates an SQL query.
//    Executes the SQL query to get a random image, name, and type from the images table.
//    If the query returns a row, sets the outputTV text to the second column of the row (the name), and initializes image to the first column of the row (the image URL).
//    Generates a list of options.
//    If currentMand is Mand.MAND1, sets the animation of the image to none.
//    If currentMand is Mand.MAND2, sets the animation of the image to a random option from the list of options.
//    If currentMand is Mand.CHILD, sets the animation of the image to a random option from the list of options1 for the first animation and a random option from the list of options2 for the second animation.
    @SuppressLint("SuspiciousIndentation", "Recycle")
    private fun testing() {
        setContentView(R.layout.activity_main_demo)
        outputTV = findViewById(R.id.idTVOutput)
        val db = dbHelper.readableDatabase
        val sql = "SELECT image,name,type FROM images ORDER BY RANDOM() LIMIT 1"
        val cursor = db.rawQuery(
            /* sql = */sql , /* selectionArgs = */
            null
            //arrayOf("cat")
        )
        if (cursor.moveToNext()) {
            outputTV.text = cursor.getString(1)
            val image = cursor.getString(0)
            speak(cursor.getString(1))
            val options = listOf("rotate", "jumping", "big", "small","red","green","blue","yellow")

            if (currentMand==Mand.MAND1) {
                Log.e("test","mand1")
                setAnimation(image, "none","none")

            }
            else if (currentMand==Mand.MAND2) {
                val randomOption = options.random()
                setAnimation(image,randomOption,"none")

            }
            else{
                val options1 = listOf("rotate", "jumping")
                val options2 = listOf("big", "small","red","green","blue","yellow")
                setAnimation(image,options1.random(),options2.random())
                //setAnimation(image,)
            }


        }
        else {

            outputTV.text ="error"
        }

    }


//    this code listens for voice input, converts it to text using the device's speech-to-text engine, and performs some action based on the recognized text.
//    The code defines a function startSpeechToText that initializes the speech recognition engine and sets up a listener to receive the results.
//    When the user starts speaking, the function sets the microphone up which indicate that it's active, and when the user stops speaking
//    The function then processes the recognized text to identify keywords such as colors, animation types, and sizes. It then retrieves an image from a database based on the recognized text and the identified keywords and displays it on the screen using various animations.


    private fun startSpeechToText() {
        outputTV = findViewById(R.id.idTVOutput)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        speechRecognizer?.startListening(intent)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle?) {
               // Toast.makeText(applicationContext, "Listening...", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float)  {

            }
            override fun onBufferReceived(bytes: ByteArray?) {}
            override fun onEndOfSpeech() {
                //Toast.makeText(applicationContext, "Stopped Listening", Toast.LENGTH_SHORT).show()
                micIV.setColorFilter(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.mic_disabled_color
                    )
                )


            }

            override fun onError(i: Int) {
                //if (i == SpeechRecognizer.ERROR_NO_SPEECH)
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val appProcesses = am.runningAppProcesses

                for (appProcess in appProcesses) {
                    if (appProcess.processName == packageName) {
                        // If the app is in the foreground, the importance will be IMPORTANCE_FOREGROUND or IMPORTANCE_VISIBLE
                        val importance = appProcess.importance
                        if (importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            || importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {


                            micIV.setColorFilter(
                                ContextCompat.getColor(
                                    applicationContext,
                                    R.color.mic_enabled_color
                                )
                            )
                            // The user has stopped speaking, so start listening again
                            if (currentMode != Mode.PARENT ) {
                                speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                            }
                        } else {

                            speechRecognizer?.stopListening()
                        }
                    }
                }

            }

            @SuppressLint("Recycle", "SetTextI18n")
            override fun onResults(bundle: Bundle) {
                val result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (result != null) {
                    val db = dbHelper.readableDatabase

                    val split_result = result[0].split(" ")
                    var color = "null"
                    var color_name = "none"
                    var rotate = false
                    var jumping = false
                    var small = false
                    var big = false
                    var last_val = "null"
                    for (i in split_result)
                    {
                        color = getColorCodeByName(i)
                        if (color!="null")
                            color_name = i
                        if (i =="spinning" || i=="spin"){
                            rotate = true
                        }
                        if (i =="jumping" || i=="bounce" || i =="jump" || i=="bouncing"){
                            jumping = true
                        }
                        if (i =="small" || i=="shrink" || i =="compress" || i=="decrease"){
                            small = true
                        }
                        if (i =="big" || i=="large" || i =="enlarge" || i=="expand" || i=="increase"){
                            big = true
                        }
                        val sql = "SELECT image,name,type FROM images WHERE name=? "
                        val cursor11 = db.rawQuery(
                            /* sql = */ sql,
                            /* selectionArgs = */ arrayOf(i)
                        )
                        if (cursor11.moveToNext())
                        {
                        last_val = i
                        }

                    }
//    Executes the SQL query to get a like image, name, and type from the images table.
                    val cursor: Cursor
                    val sql = "SELECT image,name,type FROM images WHERE name LIKE '%' || ? || '%' ORDER BY RANDOM() LIMIT 1"

                    cursor = db.rawQuery(
                            /* sql = */ sql,
                            /* selectionArgs = */ arrayOf(last_val)
                        )
                    if (cursor.moveToNext() && (currentMand!=Mand.SETMENU && currentMand!=Mand.SETLOCK) )
                    {
                        outputTV.text = last_val

                        val image = cursor.getString(0)
                        var flag = false
                        // 1 mand only text is given
                        if (currentMand==Mand.MAND1) {
                            setAnimation(image, "none","none")

                        }
                        // in mand 2 we can have object + 1 operation which can be rotate jump ...etc
                        else if (currentMand==Mand.MAND2)
                        {

                            if (rotate) {
                                setAnimation(image, "rotate","none")

                                flag = true
                            }
                            else if(jumping) {
                                setAnimation(image, "jumping","none")
                                flag = true
                            }
                            else if(big)
                            {
                                setAnimation(image, "big","none")
                                flag = true
                            }
                            else if(small)
                            {
                                setAnimation(image, "small","none")
                                flag = true
                            }
                            if (!flag)
                            {
                                val options = listOf("rotate", "jumping", "big", "small","red","green","blue","yellow")
                                setAnimation(image, options.random(), "none")

                            }



                        }
                        // in mand 3 we can have object + 2 operations which can be (rotate, big),(jump,small) ...etc
                        else if (currentMand==Mand.MAND3){
                            //speak(result[0])
                            var text1 = "none"
                            var text2 = "none"
                            if (rotate) {
                                text1 = "rotate"

                            }
                            else if(jumping) {
                                text1 = "jumping"

                            }
                            else if(big)
                            {
                                text2 = "big"

                            }
                            else if(small)
                            {
                                text2 = "small"

                            }

                            if (text1=="none" && text2=="none")
                            {
                                val options1 = listOf("rotate", "jumping","big", "small")
                                if (color_name=="none") {
                                    val options2 = listOf("red", "green", "blue", "yellow")
                                    setAnimation(image, options1.random(), options2.random())
                                }
                                else{
                                    setAnimation(image, color_name, options1.random())
                                }
                            }

                            else if (text1=="none" && text2!="none")
                            {
                                val options1 = listOf("rotate", "jumping","big", "small")
                                if (color_name=="none") {
                                    setAnimation(image, options1.random(), text2)
                                }
                                else{

                                    setAnimation(image, color_name, text2)
                                }
                            }
                            else if (text1!="none" && text2 == "none")
                            {
                                val options1 = listOf("rotate", "jumping","big", "small")
                                if (color_name=="none") {
                                    setAnimation(image, options1.random(), text1)
                                }
                                else{

                                    setAnimation(image, color_name, text1)
                                }
                            }
                            else {
                                setAnimation(image, text2, text1)
                                setAnimation(image, color_name, "none")
                            }



                        }
                        // if nothing is found we will listen again
                        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))

                    } else {
                        try {
                            //outputTV.text = result[0]
                            val sql1 = "SELECT menu_text,name FROM menu where name=? ORDER BY RANDOM() LIMIT 1" // find menu or lock then use it accordingly for command

                             val cursor1 = db.rawQuery(
                                /* sql = */sql1,
                                /* selectionArgs = */ arrayOf("menu")
                            )

                            val cursor2 = db.rawQuery(
                                /* sql = */ sql1,
                                /* selectionArgs = */arrayOf("lock")
                            )

                        if (cursor1.moveToNext() && cursor2.moveToNext() ) {
                            val menu_checked = GetPatern(cursor1.getString(0),result[0]) // find related result for menu
                            val demo_checked = GetPatern("show me",result[0]) // same for show me
                            val lock_checked = GetPatern(cursor2.getString(0),result[0])
                            if (currentMand == Mand.SETMENU) {
                                dbHelper.UpdateMenu("menu", result[0])
                                val text = "menu voice: " + result[0] + " is fixed"
                                Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                                parent()
                            }
                            else if (currentMand == Mand.SETLOCK) {
                                dbHelper.UpdateMenu("lock", result[0])
                                val text = "lock voice: " + result[0] + " is fixed"
                                Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                                parent()
                            }

                            else if (menu_checked) {
                                tapCount = 0
                                speechRecognizer?.stopListening()
                                parent()
                            } else if (demo_checked) {
                                tapCount = 0
                                speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                                if (currentMode == Mode.DEMO) {
                                    testing()
                                }


                            }
                            else if(lock_checked){
                                System.exit(0) // if you want to come out of the app
                            }
                            else if (result[0] == "close") { // close the output at any time
                                tapCount = 0
                                child()
                            }
                            else if (result[0] == cursor2.getString(0)) {
                                System.exit(0) // if you want to come out of the app
                            }

                        }


                            speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                            //speechRecognizer?.startListening(intent)
                        }
                        catch (e: Exception) {
                            Log.e("set:", "Error in set animation: ${e.message}")
                            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                        }

                    }


                }
                    // if nothing is there it will listen again
                    speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                    //speechRecognizer?.startListening(intent)



            }

            override fun onPartialResults(bundle: Bundle) {} // not using there were no requirement using it
            override fun onEvent(i: Int, bundle: Bundle?) {}// same here

        })



    }
// To checkAudioPermission
    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.RECORD_AUDIO"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:com.programmingtech.offlinespeechtotext")
            )
            startActivity(intent)
            Toast.makeText(this, "Please Allow Your Microphone Permission", Toast.LENGTH_SHORT)
                .show()
        }


    }
   // To recognise touch events
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onDown(p0: MotionEvent): Boolean {

        return true
    }

    override fun onShowPress(p0: MotionEvent) {

//        return true
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        tapCount++
        if (tapCount == 3) { // when there are 3 continuous touch close the app
            System.exit(0)
        }

        return true
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {

        return true
    }

    override fun onLongPress(p0: MotionEvent) {

        //return true
    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {

        return true
    }


}