package cz.vendasky.vikendovka

open class Location(var lat: Double, var lon: Double, var name: String, var icon: String) {
    constructor() : this(-1.0, -1.0, "", "")
}