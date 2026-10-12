import { registerRootComponent } from "expo";

const bundle = require("../ui/build/kotlin-webpack/js/productionExecutable/recife-saudavel-ui.js");
const createApp = bundle.ui?.br?.senac?.recifesaudavel?.ui?.createApp;

if (typeof createApp !== "function") {
  throw new Error("Kotlin UI bundle did not export createApp().");
}

registerRootComponent(createApp());
