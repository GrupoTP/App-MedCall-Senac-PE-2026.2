// Entry point only. The interface, navigation and data access live in Kotlin (recife-saudavel-app).
import { registerRootComponent } from "expo";

const bundle = require("../recife-saudavel-app/build/kotlin-webpack/js/productionExecutable/recife-saudavel-app.js");
const createApp = bundle["recife-saudavel-app"]?.grupotp?.recifesaudavel?.app?.createApp;

if (typeof createApp !== "function") {
  throw new Error("Kotlin UI bundle did not export createApp(). Run ./gradlew :recife-saudavel-app:buildClientBundle.");
}

registerRootComponent(createApp());
