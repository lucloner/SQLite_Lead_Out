package cn.biggee.kotlin.yj.faces.db

import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

open class SQLiteLeadOut(val path:String) {
    private val yjTABLE_NAME = arrayOf("entrance", "setting", "userNames", "inj")
    private val fileName= arrayOf(
        "yj_"+yjTABLE_NAME[0]+"_log.db",
        "yj_"+yjTABLE_NAME[2]+"_log.db",
        "face.db"
    )
    private val bdTABLE_NAME= arrayOf("user_group","user","feature","sqlite_sequence")
    private val TABLE_NAME=arrayOf(yjTABLE_NAME[0],yjTABLE_NAME[2],bdTABLE_NAME[0],bdTABLE_NAME[1],bdTABLE_NAME[2],bdTABLE_NAME[3])
    val err=StringBuffer("err msg:\n")

    private fun getConn(fileName:String):Connection{
        return DriverManager.getConnection("jdbc:sqlite:"+path+"/"+fileName)
    }

    private fun getRS(conn:Connection,sql:String):ResultSet{
        return conn.createStatement().executeQuery(sql)
    }

    private fun getConnRS(tableID: Int):ResultSet{
        return getConnRS(tableID,null)
    }

    private fun getConnRS(tableID: Int,where: String?):ResultSet{
        return getConnRS(tableID,tableID,where)
    }

    private fun getConnRS(fileID: Int,TABLE_NAME_ID: Int):ResultSet{
        return getConnRS(fileID,TABLE_NAME_ID,null)
    }

    private fun getConnRS(fileID: Int,TABLE_NAME_ID: Int,where: String?):ResultSet{
        val i=if(fileID>fileName.size-1) fileName.size-1 else fileID
        return getConnRS(fileName[i],TABLE_NAME_ID,where)
    }

    private fun getConnRS(fileName: String,TABLE_NAME_ID: Int):ResultSet{
        return getConnRS(fileName,TABLE_NAME_ID,null)
    }

    fun getConnRS(fileName: String,TABLE_NAME_ID: Int,where:String?):ResultSet{
        val i=if(TABLE_NAME_ID>TABLE_NAME.size-1) TABLE_NAME.size-1 else TABLE_NAME_ID
        return getConnRS(fileName,"SELECT * FROM "+ TABLE_NAME[i],where)
    }

    private fun getConnRS(fileName: String,sql: String):ResultSet{
        return getConnRS(fileName,sql,null)
    }

    private fun getConnRS(fileName: String,sql: String,where:String?):ResultSet{
        val s=if(where==null) sql else "$sql WHERE $where"
        return getRS(getConn(fileName),s)
    }

    private fun getStackTrace(stackTraces: Array<StackTraceElement>):String{
        val s= StringBuffer("\n")
        stackTraces.forEach {
            s.append(it.lineNumber)
            s.append(" ")
            s.append(it.isNativeMethod)
            s.append(" ")
            s.append(it.fileName)
            s.append(" ")
            s.append(it.className)
            s.append(" ")
            s.append(it.methodName)
            s.append("\t")
        }
        s.append("\n")
        return s.toString()
    }

    private fun getRow(resultSet: ResultSet):String{
        val s= StringBuffer()
        var i=0
        while (true){
            try {
                var o:String=resultSet.getObject(++i).toString()
                try {
                    o=resultSet.getString(i)
                }
                catch (e:Exception){
                    err.append(getStackTrace(e.stackTrace))
                    o=(ObjectInputStream(ByteArrayInputStream(resultSet.getBytes(i))).readObject()) as String
                }
                if(o.length<64) {
                    s.append(o)
                }
                s.append("/0")
            }
            catch (e:Exception){
                err.append(getStackTrace(e.stackTrace))
                break
            }
        }
        return s.toString()
    }

    fun readUsers():String{
        val s= StringBuffer()
        try{
            val userRS= getConnRS(4)
            while (userRS.next()){
                try{
                    val uid=userRS.getString("user_id")
                    val ctime=(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)).format(userRS.getLong("ctime"))
                    val featureRS=getConnRS(4,"user_id="+uid)
                    val imageName=featureRS.getString("image_name")
                    val userNameRs=getConnRS(1,"Uid="+uid)
                    val userName=(ObjectInputStream(ByteArrayInputStream(userNameRs.getBytes("etc"))).readObject()).toString()
                    System.out.println(uid+"\t"+imageName+"\t"+userName+"\t"+ctime)
                    s.append(getRow(userRS))
                    s.append(getRow(featureRS))
                    s.append(getRow(userNameRs))
                    s.append("\n")
                }
                catch (e:Exception){
                    err.append(getStackTrace(e.stackTrace))
                    continue
                }
            }
        }
        catch (e:Exception){
            err.append(getStackTrace(e.stackTrace))
        }
//        System.out.println("err:")
//        System.out.println(err.toString())
        return s.toString()
    }
}

fun main(args: Array<String>) {
    var path=if (args.size>0) args[0] else "/home/lucloner/source/dump/db"
    val s=SQLiteLeadOut(path).readUsers()
    System.out.println("detail:")
    System.out.println(s.replace("/0","\t"))
}
