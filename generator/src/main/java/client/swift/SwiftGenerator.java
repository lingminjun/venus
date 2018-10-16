package client.swift;

import client.Generator;
import com.venus.apigw.document.entities.CodeInfo;
import com.venus.apigw.document.entities.MethodInfo;
import com.venus.apigw.document.entities.TypeStruct;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-10-16
 * Time: 上午10:15
 */
public class SwiftGenerator extends Generator {
    protected final String prefix;

    public SwiftGenerator(String outPath, String prefix, String gwHost) {
        this(outPath,prefix,gwHost,null,null);
    }

    public SwiftGenerator(String outPath, String prefix, String gwHost, String[] filterGroups, String[] selectors) {
        this(outPath,prefix,gwHost,80,filterGroups,selectors);
    }

    public SwiftGenerator(String outPath, String prefix,String gwHost, int gwPort,  String[] filterGroups, String[] selectors) {
        super(outPath, gwHost,gwPort,"1.0.0","swift","4.0", filterGroups, selectors);
        this.prefix = (prefix == null || prefix.length() == 0) ? null : prefix;
    }

    @Override
    public String theFilePrefix() {
        return prefix;
    }

    @Override
    protected String theFileExt() {
        return ".swift";
    }

    public void genAPICall(StringBuilder builder, MethodInfo methodInfo) {
        //TODO :
    }

    public void genAPIEntity(StringBuilder builder, TypeStruct typeStruct) {
        //TODO :
    }

    public void genAPICodes(StringBuilder builder, List<CodeInfo> codeList) {
        //TODO :
    }
}
