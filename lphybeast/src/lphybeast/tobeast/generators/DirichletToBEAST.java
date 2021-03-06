package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.Dirichlet;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class DirichletToBEAST implements GeneratorToBEAST<Dirichlet, Prior> {
    @Override
    public Prior generatorToBEAST(Dirichlet generator, BEASTInterface value, BEASTContext context) {
            beast.math.distributions.Dirichlet beastDirichlet = new beast.math.distributions.Dirichlet();
            beastDirichlet.setInputValue("alpha", context.getBEASTObject(generator.getConcentration()));
            beastDirichlet.initAndValidate();

            return BEASTContext.createPrior(beastDirichlet, (RealParameter)value);
        }

    @Override
    public Class<Dirichlet> getGeneratorClass() {
        return Dirichlet.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
