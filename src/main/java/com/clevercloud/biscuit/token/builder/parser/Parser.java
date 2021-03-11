package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.builder.*;
import io.vavr.Tuple2;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.clevercloud.biscuit.token.builder.Utils.*;

public class Parser {
    public static Either<Error, Tuple2<Integer, Fact>> fact(String s){
        return fact(s,0);
    }

    public static Either<Error, Tuple2<Integer, Fact>> fact(String s, int offset) {
        Either<Error, Tuple2<Integer, Predicate>> res = predicate(s);
        if (res.isLeft()) {
            return Either.left(res.getLeft());
        } else {
            Tuple2<Integer, Predicate> t = res.get();
            return Either.right(new Tuple2<>(t._1, new Fact(t._2)));
        }
    }

    public static Either<Error, Tuple2<Integer, Rule>> rule(String s) {
        return rule(s,0);
    }

    public static Either<Error, Tuple2<Integer, Rule>> rule(String s, int offset) {
        Either<Error, Tuple2<Integer, Predicate>> res0 = predicate(s, offset);
        if (res0.isLeft()) {
            return Either.left(res0.getLeft());
        }

        Tuple2<Integer, Predicate> t0 = res0.get();
        int startIndex = t0._1;
        Predicate head = t0._2;

        int index2 = s.length();
        for (int i = startIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                index2 = i;
                break;
            }
        }
        if (index2 == s.length() || s.charAt(index2) != '<' || s.charAt(index2+1) != '-') {
            return Either.left(new Error(s.substring(startIndex), "rule arrow not found"));
        }

        List<Predicate> predicates = new ArrayList<Predicate>();
        startIndex = index2+2;
        int endIndex = s.length();
        //s = s.substring(index2+2);
        while(true) {
            int index_loop = endIndex;
            for (int i = startIndex; i < endIndex; i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            startIndex = index_loop;
            //s = s.substring(index_loop);

            Either<Error, Tuple2<Integer, Predicate>> res = predicate(s, startIndex);
            if (res.isLeft()) {
                break;
            }

            Tuple2<Integer, Predicate> t = res.get();
            startIndex = t._1;
            predicates.add(t._2);

            index_loop = endIndex;
            for (int i = startIndex; i < endIndex; i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            
            if(index_loop == endIndex || s.charAt(index_loop) != ',') {
                //s = s.substring(index_loop);
                startIndex = index_loop;
                break;
            } else {
                startIndex = index_loop + 1;
                //s = s.substring(index_loop + 1);
            }
        }

        //FIXME: handle constraints

        return Either.right(new Tuple2<>(startIndex, Utils.rule(head.getName(), head.getIds(), predicates)));
    }

    public static Either<Error, Tuple2<Integer, Check>> check(String s) {
        return Either.left(new Error(s, "unimplemented"));
    }

    public static Either<Error, Tuple2<Integer, Predicate>> predicate(String s){
        return predicate(s,0);
    }

    public static Either<Error, Tuple2<Integer, Predicate>> predicate(String s, int offset) {
        int index = s.length();
        for (int i = offset; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!(Character.isAlphabetic(c) || c == '_')) {
                if (i == offset) {
                    return Either.left(new Error(s.substring(offset), "empty name"));
                } else {
                    index = i;
                    break;
                }
            }
        }

        if (index == s.length()) {
            return Either.left(new Error(s.substring(offset), "end of name not found"));
        }

        String name = s.substring(offset, index);

        int index2 = s.length();
        for (int i = index; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                index2 = i;
                break;
            }
        }
        if (index2 == s.length() || s.charAt(index2) != '(') {
            return Either.left(new Error(s.substring(offset), "opening parens not found"));
        }

        List<Term> terms = new ArrayList<Term>();
        int startIndex = index2+1;
        // s = s.substring(index2+1);
        while(true) {
            int index_loop = s.length();
            for (int i = startIndex; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            startIndex = index_loop;
            //s = s.substring(index_loop);

            Either<Error, Tuple2<Integer, Term>> res = atom(s, startIndex);
            if (res.isLeft()) {
                break;
            }

            Tuple2<Integer, Term> t = res.get();
            startIndex = t._1;
            terms.add(t._2);

            index_loop = s.length();
            for (int i = startIndex; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            if(s.charAt(index_loop) != ',') {
                startIndex = index_loop;
                // s = s.substring(index_loop);
                break;
            } else {
                startIndex = index_loop + 1;
                // s = s.substring(index_loop + 1);
            }
        }

        int index3 = s.length();
        for (int i = startIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                index3 = i;
                break;
            }
        }

        if (index3 == s.length() || s.charAt(index3) != ')') {
            return Either.left(new Error(s.substring(startIndex), "closing parens not found"));
        }

        return Either.right(new Tuple2<Integer, Predicate>(index3 + 1, new Predicate(name, terms)));
    }

    public static Either<Error, Tuple2<Integer, String>> name(String s) {
        return name(s,0);
    }

    public static Either<Error, Tuple2<Integer, String>> name(String s, int offset) {
        int index = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!(Character.isAlphabetic(c) || c == '_')) {
                index = i;
                break;
            }
        }

        if(index == 0) {
            return Either.left(new Error(s, "empty name"));
        }
        String name = s.substring(0, index);
        return Either.right(new Tuple2<Integer, String>(index, name));
    }

    public static Either<Error, Tuple2<Integer, Term>> atom(String s, int offset) {
        Either<Error, Tuple2<Integer, Term.Symbol>> res1 = symbol(s, offset);
        if(res1.isRight()) {
            Tuple2<Integer, Term.Symbol> t = res1.get();
            return Either.right(new Tuple2<Integer, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Integer, Term.Str>> res2 = string(s, offset);
        if(res2.isRight()) {
            Tuple2<Integer, Term.Str> t = res2.get();
            return Either.right(new Tuple2<Integer, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Integer, Term.Integer>> res3 = integer(s, offset);
        if(res3.isRight()) {
            Tuple2<Integer, Term.Integer> t = res3.get();
            return Either.right(new Tuple2<Integer, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Integer, Term.Date>> res4 = date(s, offset);
        if(res4.isRight()) {
            Tuple2<Integer, Term.Date> t = res4.get();
            return Either.right(new Tuple2<Integer, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Integer, Term.Variable>> res5 = variable(s, offset);
        if(res5.isRight()) {
            Tuple2<Integer, Term.Variable> t = res5.get();
            return Either.right(new Tuple2<Integer, Term>(t._1, t._2));
        }

        return Either.left(new Error(s, "unrecognized value"));
    }

    public static Either<Error, Tuple2<Integer, Term.Symbol>> symbol(String s){
        return symbol(s,0);
    }

    public static Either<Error, Tuple2<Integer, Term.Symbol>> symbol(String s, int offset) {
        if(s.charAt(offset) !='#') {
            return Either.left(new Error(s, "not a symbol"));
        }
        int secondOffset = offset + 1;

        int index = s.length();
        for (int i = secondOffset; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!(Character.isAlphabetic(c) && c != '_')) {
                index = i;
                break;
            }
        }

        if(index == secondOffset) {
            return Either.left(new Error(s, "empty symbol"));
        }
        String name = s.substring(secondOffset, index);
        return Either.right(new Tuple2<Integer, Term.Symbol>(index, (Term.Symbol) s(name)));
    }

    public static Either<Error, Tuple2<Integer, Term.Str>> string(String s) {
        return string(s,0);
    }

    public static Either<Error, Tuple2<Integer, Term.Str>> string(String s, int offset) {
        if(s.charAt(offset) !='"') {
            return Either.left(new Error(s.substring(offset), "not a string"));
        }

        int index = s.length();
        for (int i = offset + 1; i < s.length(); i++) {
            char c = s.charAt(i);

            if(c == '\\' && s.charAt(i+1) == '"') {
                i += 1;
                continue;
            }

            if (c == '"') {
                index = i-1;
                break;
            }
        }

        if(index == s.length()) {
            return Either.left(new Error(s, "end of string not found"));
        }

        if (s.charAt(index+1) != '"'){
            return Either.left(new Error(s, "ending double quote not found"));
        }

        String string = s.substring(offset+1, index+1);
        return Either.right(new Tuple2<Integer, Term.Str>(index +2 , (Term.Str) Utils.string(string)));
    }

    public static Either<Error, Tuple2<Integer, Term.Integer>> integer(String s) {
        return integer(s,0);
    }

    public static Either<Error, Tuple2<Integer, Term.Integer>> integer(String s, int offset) {
        int index = offset;
        if(s.charAt(offset) == '-') {
            index += 1;
        }

        int index2 = s.length();
        for (int i = index; i < s.length(); i++) {
            char c = s.charAt(i);

            if(!Character.isDigit(c)) {
                index2 = i;
                break;
            }
        }

        if (index2 == offset) {
            return Either.left(new Error(s, "not an integer"));
        }

        Integer i = Integer.parseInt(s.substring(offset, index2));
        return Either.right(new Tuple2<Integer, Term.Integer>(index2, (Term.Integer) Utils.integer(i.intValue())));
    }

    public static Either<Error, Tuple2<Integer, Term.Date>> date(String s){
        return date(s,0);
    }

    public static Either<Error, Tuple2<Integer, Term.Date>> date(String s, int offset) {
        int index = s.length();
        for (int i = offset; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == ',' || c == ')') {
                index = i;
                break;
            }
        }

        try {
            Instant i = Instant.parse(s.substring(offset, index));
            return Either.right(new Tuple2<Integer, Term.Date>(index, new Term.Date(i.getEpochSecond())));
        } catch (DateTimeParseException e) {
            return Either.left(new Error(s.substring(offset), "not a date"));

        }
    }

    public static Either<Error, Tuple2<Integer, Term.Variable>> variable(String s){
        return variable(s,0);
    }

    public static Either<Error, Tuple2<Integer, Term.Variable>> variable(String s, int offset) {
        if(s.charAt(offset) !='$') {
            return Either.left(new Error(s.substring(offset), "not a variable"));
        }

        int index = s.length();
        for (int i = offset + 1; i < s.length(); i++) {
            char c = s.charAt(i);

            if(!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
                index = i;
                break;
            }
        }

        String name = s.substring(offset + 1, index);
        return Either.right(new Tuple2<Integer, Term.Variable>(index, (Term.Variable) var(name)));
    }

    //private static Either<Error, Tuple2<Int,>>



    public static Either<Error, Tuple2<Integer, Expression>> expression(String s) {
        return expression(s, 0);
    }

    public static Either<Error, Tuple2<Integer, Expression>> expression(String s, int offset) {
        return Either.left(new Error(s.substring(offset), "unimplemented"));
    }
}
