package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.Error.Parser;
import org.biscuitsec.biscuit.token.builder.Expression;
import org.biscuitsec.biscuit.token.builder.Term;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.biscuitsec.biscuit.token.builder.Utils.constrained_rule;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorizerTest {

    @Test
    public void testAuthorizerPolicy() throws Parser {
        Authorizer authorizer = new Authorizer();
        List<Policy> policies = authorizer.policies;
        authorizer.deny();
        assertEquals(1, policies.size());

        authorizer.add_policy(new Policy(
                Arrays.asList(
                        constrained_rule(
                                "deny",
                                new ArrayList<>(),
                                new ArrayList<>(),
                                Arrays.asList(new Expression.Value(new Term.Bool(true)))
                        )
                ), Policy.Kind.Deny));
        assertEquals(2, policies.size());

        authorizer.add_policy("deny if true");
        assertEquals(3, policies.size());
    }


    @Test
    public void testPuttingSomeFactsInABiscuitAndGettingThemBackOutAgain() throws Exception {

        KeyPair keypair = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, new SecureRandom());

        Biscuit token = Biscuit.builder(keypair)
                .add_authority_fact("email(\"bob@example.com\")")
                .add_authority_fact("id(123)")
                .add_authority_fact("enabled(true)")
                .add_authority_fact("perms([1,2,3])")
                .build();

        Authorizer authorizer = Biscuit.from_b64url(token.serialize_b64url(), keypair.public_key())
                .verify(keypair.public_key())
                .authorizer();

        Term emailTerm = queryFirstResult(authorizer, "emailfact($name) <- email($name)");
        assertEquals("bob@example.com", ((Term.Str) emailTerm).getValue());

        Term idTerm = queryFirstResult(authorizer, "idfact($name) <- id($name)");
        assertEquals(123, ((Term.Integer) idTerm).getValue());

        Term enabledTerm = queryFirstResult(authorizer, "enabledfact($name) <- enabled($name)");
        assertEquals(true, ((Term.Bool) enabledTerm).getValue());

        Term permsTerm = queryFirstResult(authorizer, "permsfact($name) <- perms($name)");
        assertEquals(
                Set.of(new Term.Integer(1), new Term.Integer(2), new Term.Integer(3)),
                ((Term.Set) permsTerm).getValue()
        );
    }

    private static Term queryFirstResult(Authorizer authorizer, String query) throws Error {
        return authorizer.query(query)
                .iterator()
                .next()
                .terms().get(0);
    }
}
