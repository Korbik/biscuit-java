package com.clevercloud.biscuit.crypto;

import biscuit.format.schema.Schema;
import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.error.Error;
import com.google.protobuf.ByteString;
import io.vavr.control.Either;

import java.security.SecureRandom;
import java.util.ArrayList;

public class Token {
    public final ArrayList<byte[]> blocks; //= new ArrayList<>();
    public final ArrayList<RistrettoElement> keys; // keys = new ArrayList<>();
    public final TokenSignature signature;

    public Token(final SecureRandom rng, KeyPair keypair, byte[] message) {
        this.signature = new TokenSignature(rng, keypair, message);
        this.blocks = new ArrayList<>();
        this.blocks.add(message);
        this.keys = new ArrayList<>();
        this.keys.add(keypair.public_key);
    }

    public Token(final ArrayList<byte[]> blocks, final ArrayList<RistrettoElement> keys, final TokenSignature signature) {
        this.signature = signature;
        this.blocks = blocks;
        this.keys = keys;
    }

    public Token append(final SecureRandom rng, KeyPair keypair, byte[] message) {
        TokenSignature signature = this.signature.sign(rng, keypair, message);


        Token token = new Token(this.blocks, this.keys, signature);
        token.blocks.add(message);
        token.keys.add(keypair.public_key);

        return token;
    }

    // FIXME: rust version returns a Result<(), error::Signature>
    public Either<Error, Void> verify() {
        return this.signature.verify(this.keys, this.blocks);
    }
}