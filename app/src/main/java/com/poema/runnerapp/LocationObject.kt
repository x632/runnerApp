package com.poema.runnerapp

import com.google.firebase.firestore.GeoPoint

data class LocationObject (
    var id : String? = null,
    var locLatLng : GeoPoint? = null,
    var accDistance : Double? = null,
    var time : Int? = null)