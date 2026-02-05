import {} from "@acidify/yogurt-script-api";

// Capabilities available in the Yogurt Scripting environment
console.log(
  await http.request("https://api.github.com/repos/lagrangedev/acidify"),
);

// Call Milky API from JS environment
console.log("Login info:", JSON.stringify(await yogurt.api.get_login_info()));

// Listen for message receive events
yogurt.event.on("message_receive", (event) => {
  console.log("Received message:", JSON.stringify(event.data, null, 2));
});
