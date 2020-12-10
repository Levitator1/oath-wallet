.PHONY:all
all: oath_wallet_extension.firefox.xpi

# Note that -j flattens the archive to a single directory
oath_wallet_extension.firefox.xpi: src/manifest.json src/oath-wallet-main.js
	zip -j -r -FS "$@" $^

