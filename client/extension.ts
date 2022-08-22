import path = require("path");
import { ExtensionContext, workspace } from "vscode";
import { LanguageClient, LanguageClientOptions, ServerOptions } from "vscode-languageclient/node";

let client: LanguageClient | undefined;

export function activate(context: ExtensionContext) {
	const nbts = context.asAbsolutePath(
		path.join(
			"build",
			"install",
			"nbts",
			"bin",
			`nbts${process.platform === "win32" ? ".bat" : ""}`,
		)
	);
	const serverOptions: ServerOptions = {
		command: nbts,
		args: ["launch"],
	};
	const clientOptions: LanguageClientOptions = {
		documentSelector: [{ scheme: "file", language: "nbtscript" }],
		synchronize: {
			fileEvents: workspace.createFileSystemWatcher("**/*.nbts"),
		},
	};
	client = new LanguageClient(
		"nbtscript",
		"NbtScript",
		serverOptions,
		clientOptions,
	);
	client.start();
}

export function deactivate() {
	return client?.stop();
}
