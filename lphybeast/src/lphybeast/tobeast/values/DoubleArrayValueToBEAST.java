package lphybeast.tobeast.values;

import beast.core.parameter.RealParameter;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.LogNormalMulti;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.Arrays;
import java.util.List;

public class DoubleArrayValueToBEAST implements ValueToBEAST<Double[], RealParameter> {

    @Override
    public RealParameter valueToBEAST(Value<Double[]> value, BEASTContext context) {

        RealParameter parameter = new RealParameter();
        List<Double> values = Arrays.asList(value.value());
        parameter.setInputValue("value", values);
        parameter.setInputValue("dimension", values.size());

        // check domain
        if (value.getGenerator() instanceof Dirichlet) {
            parameter.setInputValue("lower", 0.0);
            parameter.setInputValue("upper", 1.0);
        } else if (value.getGenerator() instanceof LogNormalMulti) {
            parameter.setInputValue("lower", 0.0);
        }

        parameter.initAndValidate();
        ValueToParameter.setID(parameter, value);
        return parameter;
    }

    @Override
    public Class getValueClass() {
        return Double[].class;
    }

    @Override
    public Class<RealParameter> getBEASTClass() {
        return RealParameter.class;
    }

}
