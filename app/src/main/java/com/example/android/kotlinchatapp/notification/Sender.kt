package com.example.android.kotlinchatapp.notification

class Sender {
    lateinit var data:Data
    lateinit var to:String

    constructor(data: Data, to: String) {
        this.data = data
        this.to = to
    }
}