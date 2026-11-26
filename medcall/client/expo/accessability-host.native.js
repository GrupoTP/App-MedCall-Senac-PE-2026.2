import React, { useState } from "react";
import { StyleSheet, View } from "react-native";
import { WebView } from "react-native-webview";
import Constants from "expo-constants";

const widgetDocument = (url) => `<!DOCTYPE html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
      html, body { margin: 0; height: 100%; background: transparent; pointer-events: none; }
      #a11y-btn, #a11y-panel, #a11y-panel * { pointer-events: auto; }
    </style>
  </head>
  <body>
    <script src="${url}"></script>
    <script>
      new MutationObserver(function () {
        var panel = document.getElementById("a11y-panel");
        var open = !!(panel && panel.classList.contains("open"));
        if (window.ReactNativeWebView) {
          window.ReactNativeWebView.postMessage(open ? "open" : "closed");
        }
      }).observe(document.documentElement, { subtree: true, attributes: true, attributeFilter: ["class"] });
    </script>
  </body>
</html>`;

export function wrapWithAccessAbility(App) {
  return function AccessAbilityRoot() {
    const url = Constants.expoConfig?.extra?.accessabilityWidgetUrl;
    const [open, setOpen] = useState(false);
    if (!url) {
      return React.createElement(App, null);
    }
    return React.createElement(
      View,
      { style: styles.screen },
      React.createElement(App, null),
      React.createElement(WebView, {
        originWhitelist: ["*"],
        source: { html: widgetDocument(url), baseUrl: "https://cdn.jsdelivr.net/" },
        style: open ? styles.open : styles.button,
        containerStyle: styles.transparent,
        androidLayerType: "software",
        setSupportMultipleWindows: false,
        onMessage: (event) => setOpen(event.nativeEvent.data === "open"),
      }),
    );
  };
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  transparent: { backgroundColor: "transparent" },
  button: {
    position: "absolute",
    width: 96,
    height: 96,
    right: 8,
    bottom: 8,
    backgroundColor: "transparent",
  },
  open: {
    position: "absolute",
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    backgroundColor: "transparent",
  },
});
