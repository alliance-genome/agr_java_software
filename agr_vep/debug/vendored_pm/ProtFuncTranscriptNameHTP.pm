=head1 NAME

ProtFuncTranscriptNameHTP

=head SYNOPSIS

 cp ProtFuncTranscriptNameHTP.pm ~/.vep/Plugins/
 ./vep -i variations.vcf --plugin ProtFuncTranscriptNameHTP,mod=MOD,pass=pword

=head1

  A VEP plugin that adds the transcript name from the GFF attributes and the protein
  function annotation from SIFT and PolyPhen2

=cut

package ProtFuncTranscriptNameHTP;

use strict;
use warnings;

use Bio::EnsEMBL::Variation::ProteinFunctionPredictionMatrix qw($AA_LOOKUP);
use Bio::Seq;
use DBI;
use Digest::MD5 qw(md5_hex);

use base qw(Bio::EnsEMBL::Variation::Utils::BaseVepPlugin);

my %INCLUDE_SO = map {$_ => 1} qw(missense_variant stop_lost stop_gained start_lost);

sub new {
    my $class = shift;

    my $self = $class->SUPER::new(@_);
    my $param_hash = $self->params_to_hash();

    my $mod = $param_hash->{mod};
    my $db = $ENV{'PATH_PRED_DB_PREFIX'} . $mod;
    my $host = $ENV{'VEP_DBHOST'};
    my $user = $ENV{'VEP_DBUSER'};
    my $port = $ENV{'VEP_DBPORT'};
    $self->{pass} = $param_hash->{'pass'};

    $self->{pf_query} = qq{
        SELECT t.translation_md5, a.analysis, p.prediction_matrix
            FROM translation_md5 t
            INNER JOIN protein_function_prediction p
                ON t.translation_md5_id = p.translation_md5_id
            INNER JOIN analysis a
                ON a.analysis_id = p.analysis_id
            WHERE t.translation_md5 = ?
    };
    $self->{tn_query} = "SELECT transcript_name FROM transcript_map WHERE transcript_id = ?";

    $self->{dsn} = 'dbi:mysql:database=' . $db . ';host=' . $host . ';port=' . $port;
    $self->{user} = $user;
    $self->{dbh} ||= DBI->connect($self->{dsn}, $user, $self->{pass}) or die $DBI::errstr;
    $self->{get_pf_sth} = $self->{dbh}->prepare($self->{pf_query});
    $self->{get_tn_sth} = $self->{dbh}->prepare($self->{tn_query});

    $self->{initial_pid} = $$;

    return $self;
}


sub get_header_info {
    my $self = shift;

    return {
	transcript_name => 'Transcript name obtained from GFF',
	SIFT_prediction => 'SIFT prediction',
	SIFT_score => 'SIFT score',
	PolyPhen_prediction => 'PolyPhen-2 HumDiv prediction',
	PolyPhen_score => 'PolyPhen-2 HumDiv score'
    };
}
	

sub feature_types {
    return ['Transcript'];
}


sub variant_feature_types {
    return ['VariationFeature'];
}


sub run {
    my ($self, $tva) = @_;

    my $tr = $tva->transcript;

    my $transcript_name = $self->fetch_from_tn_cache($tr->stable_id);

    if (!$transcript_name) {
	if ($$ != $self->{initial_pid}) { #forked, reconnect to DB
	    $self->{dbh} = DBI->connect($self->{dsn}, $self->{db_user}, $self->{db_pass});
	    $self->{get_tn_sth} = $self->{dbh}->prepare($self->{tn_query});
	    $self->{get_pf_sth} = $self->{dbh}->prepare($self->{pf_query});

	    #set this so only do once per fork
	    $self->{initial_pid} = $$;
	}

	$self->{get_tn_sth}->execute($tr->stable_id);

	while (my $arrayref = $self->{get_tn_sth}->fetchrow_arrayref) {
	    $transcript_name = $arrayref->[0];
	}
	$self->add_to_tn_cache($tr->stable_id, $transcript_name);
    }

    my $results = {transcript_name => $transcript_name};

    return $results unless grep {$INCLUDE_SO{$_->SO_term}} @{$tva->get_all_OverlapConsequences};
    return $results unless $tva->variation_feature->{start} eq $tva->variation_feature->{end};
    
    my $tv = $tva->transcript_variation;

    return $results unless defined $AA_LOOKUP->{$tva->peptide} and $tv->translation_start == $tv->translation_end;

    my $tr_vep_cache = $tr->{_variation_effect_feature_cache} || {}; 

    unless ($tr_vep_cache->{peptide}) {
	my $translation = $tr->translate;
	return $results unless $translation;
	$tr_vep_cache->{peptide} = $translation->seq;
    }
    
    # get data, indexed on md5 of peptide sequence
    my $md5 = md5_hex($tr_vep_cache->{peptide});
    my $data = $self->fetch_from_pf_cache($md5);

    unless ($data) {
        # forked, reconnect to DB
	if($$ != $self->{initial_pid}) {
	    $self->{dbh} = DBI->connect($self->{dsn},$self->{user},$self->{pass});
	    $self->{get_pf_sth} = $self->{dbh}->prepare($self->{pf_query});
		
	    # set this so only do once per fork
	    $self->{initial_pid} = $$;
	}
	
	$self->{get_pf_sth}->execute($md5);

	$data = {};
	while(my $arrayref = $self->{get_pf_sth}->fetchrow_arrayref) {
	    my $analysis = $arrayref->[1];
	    # we may want to change the logic here if we want to use humvar for human variants
	    next if $analysis eq 'pph_humvar'; 
	    $analysis = $analysis eq 'pph' ? 'polyphen' : 'sift';
	    my $sub_analysis;
	    $sub_analysis = 'humdiv' if $analysis eq 'polyphen';
	    $data->{$analysis} = Bio::EnsEMBL::Variation::ProteinFunctionPredictionMatrix->new(
		-translation_md5    => $arrayref->[0],
		-analysis           => $analysis,
		-sub_analysis       => $sub_analysis,
		-matrix             => $arrayref->[2]
		);
	}
	$self->add_to_pf_cache($md5, $data);
    }

    $tr_vep_cache->{protein_function_predictions} = $data;

    foreach my $tool_string(qw(SIFT PolyPhen)) {
	my $analysis = lc($tool_string);
	next unless exists $data->{$analysis};
	my ($pred, $score) = $data->{$analysis}->get_prediction($tv->translation_start, $tva->peptide);
	if($pred) {
	    $pred =~ s/\s+/\_/g;
	    $pred =~ s/\_\-\_/\_/g;
	    $results->{$tool_string . '_prediction'} = $pred;
	    $results->{$tool_string . '_score'} = $score;
	}
    }    
   
    return $results;
}


sub fetch_from_pf_cache {
    my ($self, $md5) = @_;

    my $cache = $self->{_protein_function_cache} ||= [];
    my ($data) = map {$_->{data}} grep {$_->{md5} eq $md5} @$cache;

    return $data;
}

sub add_to_pf_cache {
    my ($self, $md5, $data) = @_;

    my $cache = $self->{_protein_function_cache} ||= [];
    push @$cache, {md5 => $md5, data => $data};

    shift @$cache while scalar @$cache > 50;

    return;
}


sub fetch_from_tn_cache {
    my ($self, $transcript_id) = @_;

    my $cache = $self->{_transcript_id_name_map_cache} ||= [];

    my ($data) = map {$_->{name}} grep {$_->{id} eq $transcript_id} @$cache;

    return $data;
}


sub add_to_tn_cache {
    my ($self, $transcript_id, $transcript_name) = @_;

    my $cache = $self->{_transcript_id_name_map_cache} ||= [];
    push @$cache, {id => $transcript_id, name => $transcript_name};

    shift @$cache while scalar @$cache > 50;

    return;
}


1;
