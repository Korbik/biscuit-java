package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.LogicError;
import io.vavr.control.Either;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class World implements Serializable {
   private final Set<Fact> facts;
   private final List<Rule> rules;
   private final List<Caveat> caveats;

   public void add_fact(final Fact fact) {
      this.facts.add(fact);
   }

   public void add_rule(final Rule rule) {
      this.rules.add(rule);
   }

   public void add_caveat(Caveat caveat) { this.caveats.add(caveat); }

   public void clearRules() {
      this.rules.clear();
   }

   public Either<Error, Void> run() {
      return this.run(new RunLimits());
   }

   public Either<Error, Void> run(RunLimits limits) {
      int iterations = 0;
      Instant limit = Instant.now().plus(limits.maxTime);

      while(true) {
         final Set<Fact> new_facts = new HashSet<>();
         for (final Rule rule : this.rules) {
            rule.apply(this.facts, new_facts);

            if(Instant.now().compareTo(limit) >= 0) {
               return Left(new Error.Timeout());
            }
         }

         final int len = this.facts.size();
         this.facts.addAll(new_facts);
         if (this.facts.size() == len) {
            return Right(null);
         }

         if (this.facts.size() >= limits.maxFacts) {
            return Left(new Error.TooManyFacts());
         }

         iterations += 1;
         if(iterations >= limits.maxIterations) {
            return Left(new Error.TooManyIterations());
         }
      }
   }

   public final Set<Fact> facts() {
      return this.facts;
   }

   public List<Rule> rules() { return this.rules; }

   public List<Caveat> caveats() { return this.caveats; }

   public final Set<Fact> query(final Predicate pred) {
      return this.facts.stream().filter((f) -> {
         if (f.predicate().name() != pred.name()) {
            return false;
         }
         final int min_size = Math.min(f.predicate().ids().size(), pred.ids().size());
         for (int i = 0; i < min_size; ++i) {
            final ID fid = f.predicate().ids().get(i);
            final ID pid = pred.ids().get(i);
            if ((fid instanceof ID.Symbol || fid instanceof ID.Integer || fid instanceof ID.Str || fid instanceof ID.Date)
                    && fid.getClass() == pid.getClass()) {
               if (!fid.equals(pid)) {
                  return false;
               }
            } else if (!(fid instanceof ID.Symbol && pid instanceof ID.Variable)) {
               return false;
            }
         }
         return true;
      }).collect(Collectors.toSet());
   }

   public final Set<Fact> query_rule(final Rule rule) {
      final Set<Fact> new_facts = new HashSet<>();
      rule.apply(this.facts, new_facts);
      return new_facts;
   }

   public World() {
      this.facts = new HashSet<>();
      this.rules = new ArrayList<>();
      this.caveats = new ArrayList<>();
   }

   public World(Set<Fact> facts, List<Rule> rules) {
      this.facts = facts;
      this.rules = rules;
      this.caveats = new ArrayList<>();
   }

   public World(Set<Fact> facts, List<Rule> rules, List<Caveat> caveats) {
      this.facts = facts;
      this.rules = rules;
      this.caveats = caveats;
   }

   public World(World w) {
      this.facts = new HashSet<>();
      for(Fact fact: w.facts) {
         this.facts.add(fact);
      }
      this.rules = new ArrayList<>();
      for(Rule rule: w.rules) {
         this.rules.add(rule);
      }
      this.caveats = new ArrayList<>();
      for(Caveat caveat: w.caveats) {
         this.caveats.add(caveat);
      }
   }

   public String print(SymbolTable symbol_table) {
      StringBuilder s = new StringBuilder();

      s.append("World {\n\t\tfacts: [");
      for(Fact f: this.facts) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_fact(f));
      }
      s.append("\n\t\t]\n\t\trules: [");
      for(Rule r: this.rules) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_rule(r));
      }
      s.append("\n\t\t]\n\t\tcaveats: [");
      for(Caveat c: this.caveats) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_caveat(c));
      }
      s.append("\n\t\t]\n\t}");

      return s.toString();
   }
}
