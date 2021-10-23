package com.transloadit.examples
import com.transloadit.sdk.Assembly
import com.transloadit.sdk.AssemblyListener
import com.transloadit.sdk.Transloadit
import com.transloadit.sdk.response.AssemblyResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


/**
 * This Kotlin Class provides a brief example of how to set up an Assembly using Kotlin.
 * This Assembly has only one step in which the input files are modified as following:
 *  -   applying a sepia filter on each image
 *  -   adding a text to the images
 *  -   reduce quality to 80 percent and convert to a .PNG file
 */
class KotlinExample {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Setup the TransloaditClient and a new Assembly
            val transloadit = Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET")

            val assembly: Assembly = transloadit.newAssembly()
            val kotlinExample = KotlinExample()

            // Load Files and add the to the Assembly
            val file1 = File(KotlinExample::class.java.getResource("/lol_cat.jpg").file)
            val file2 = File(KotlinExample::class.java.getResource("/mona_lisa.jpg").file)
            assembly.addFile(file1)
            assembly.addFile(file2)

            // Define Steps and add them to the Assembly
            val textOptions = JSONArray().put(
                    JSONObject()
                    .put("text","Kotlin Example")
                    .put("color","#e61010")
                    .put("font","Helvetica")
                    .put("size", 15)
            )

            val stepOptions = hashMapOf<String, Any>("format" to "png", "quality" to 80, "sepia" to 95, "text" to textOptions )

            assembly.addStep("convert_and_text", "/image/resize", stepOptions);

            // Add Assembly Listener to receive notifications.
            assembly.assemblyListener = object : AssemblyListener {
                override fun onAssemblyFinished(response: AssemblyResponse?) {
                    println("Download Results")
                    if (response != null) {
                       val resultJson: JSONArray = response.getStepResult("convert_and_text");
                        for ( i in 0 until resultJson.length()){
                            val resultFile: JSONObject = resultJson.getJSONObject(i)
                            println(resultFile.getString("basename") + "." + resultFile.get("ext")
                                    + " : " + resultFile.getString("ssl_url"))
                        }
                    } else {
                        onError(error = NullPointerException())
                    }
                }

                override fun onError(error: Exception?) {
                    println("Assembly failed!")
                    print(error.toString())
                }

                override fun onMetadataExtracted() {
                    println("Metadata_Extracted")
                }

                override fun onAssemblyUploadFinished() {
                    println("Assembly uploaded, executing ...")
                }

                override fun onFileUploadFinished(fileName: String?, uploadInformation: JSONObject?) {
                    println("File " + fileName + "has been uploaded successfully")
                }

                override fun onAssemblyResultFinished(stepName: String?, result: JSONObject?) {
                    println("Step Result available: ")
                    println(
                        """StepName: $stepName File: ${result!!.getString("basename")}.${result.getString("ext")}
                    """.trimIndent())
                }
            }

            // Start the Assembly
            assembly.save()
        }
    }
}