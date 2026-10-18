// Entry point only. The interface, navigation and data access live in Kotlin (client/app).
import { registerRootComponent } from "expo";

const bundle = require("../app/build/kotlin-webpack/js/productionExecutable/app.js");
const createApp = bundle.grupotp?.recifesaudavel?.app?.createApp;

if (typeof createApp !== "function") {
  throw new Error("Kotlin UI bundle did not export createApp(). Run ./gradlew :client:app:buildClientBundle.");
}

registerRootComponent(createApp());
